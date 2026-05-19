package main

import (
	"crypto/hmac"
	"crypto/sha256"
	_ "embed"
	"encoding/binary"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"os"
	"regexp"
	"strings"
)

//go:embed hmac.key
var hmacKeyHex string

var magic = []byte{'M', 'S', 'T', 'U', 'P', 0x01, 0x00, 0x00}

const (
	hmacLen      = 32
	lenField     = 4
	trailerFixed = 8 + lenField + hmacLen
	maxPayload   = 8192
	maxApps      = 50
)

var idPattern = regexp.MustCompile(`^[A-Za-z0-9][A-Za-z0-9+.\-]{0,62}[A-Za-z0-9+]$`)

type payload struct {
	V    int      `json:"v"`
	Apps []string `json:"apps"`
}

func readTrailer() ([]string, error) {
	exePath, err := os.Executable()
	if err != nil {
		return nil, err
	}

	f, err := os.Open(exePath)
	if err != nil {
		return nil, err
	}
	defer f.Close()

	info, err := f.Stat()
	if err != nil {
		return nil, err
	}
	if info.Size() < int64(trailerFixed) {
		return nil, errors.New("instalador incompleto")
	}

	gotMagic := make([]byte, 8)
	if _, err := f.ReadAt(gotMagic, info.Size()-8); err != nil {
		return nil, err
	}
	if !hmac.Equal(gotMagic, magic) {
		return nil, errors.New("instalador sem lista de aplicativos")
	}

	lenBytes := make([]byte, lenField)
	if _, err := f.ReadAt(lenBytes, info.Size()-8-int64(lenField)); err != nil {
		return nil, err
	}
	payloadLen := binary.LittleEndian.Uint32(lenBytes)
	if payloadLen == 0 || payloadLen > maxPayload {
		return nil, errors.New("instalador adulterado")
	}

	total := int64(trailerFixed) + int64(payloadLen)
	if info.Size() < total {
		return nil, errors.New("instalador adulterado")
	}

	gotHmac := make([]byte, hmacLen)
	if _, err := f.ReadAt(gotHmac, info.Size()-8-int64(lenField)-int64(hmacLen)); err != nil {
		return nil, err
	}

	payloadBytes := make([]byte, payloadLen)
	if _, err := f.ReadAt(payloadBytes, info.Size()-total); err != nil {
		if err != io.EOF {
			return nil, err
		}
	}

	key, err := hex.DecodeString(strings.TrimSpace(hmacKeyHex))
	if err != nil || len(key) != hmacLen {
		return nil, errors.New("chave HMAC invalida no binario")
	}

	mac := hmac.New(sha256.New, key)
	mac.Write(magic)
	mac.Write(lenBytes)
	mac.Write(payloadBytes)
	expected := mac.Sum(nil)
	if !hmac.Equal(expected, gotHmac) {
		return nil, errors.New("instalador adulterado")
	}

	var p payload
	if err := json.Unmarshal(payloadBytes, &p); err != nil {
		return nil, errors.New("instalador adulterado")
	}
	if p.V != 1 {
		return nil, fmt.Errorf("versao de payload nao suportada: %d", p.V)
	}
	if len(p.Apps) == 0 || len(p.Apps) > maxApps {
		return nil, errors.New("lista de aplicativos invalida")
	}
	for _, id := range p.Apps {
		if !isValidAppID(id) {
			return nil, errors.New("ID de aplicativo invalido")
		}
	}

	return p.Apps, nil
}

func isValidAppID(id string) bool {
	if strings.Contains(id, "..") {
		return false
	}
	return idPattern.MatchString(id)
}
