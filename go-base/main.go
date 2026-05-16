package main

import (
	"bufio"
	"fmt"
	"os"
	"os/exec"
	"strings"
)

const startMarker = "||APPS:"
const endMarker = "||"

func main() {
	fmt.Println("============================================")
	fmt.Println("   MeuSetup - Instalador Automático")
	fmt.Println("============================================")
	fmt.Println()

	apps, err := extractApps()
	if err != nil {
		fmt.Println("Erro: este executável não contém uma lista de aplicativos.")
		fmt.Println("Gere um instalador personalizado em nosso site.")
		pause()
		os.Exit(1)
	}

	if len(apps) == 0 {
		fmt.Println("Nenhum aplicativo foi selecionado.")
		pause()
		return
	}

	fmt.Printf("Instalando %d aplicativo(s) via Winget...\n\n", len(apps))

	failed := []string{}
	for i, app := range apps {
		fmt.Printf("──────────────────────────────────────────\n")
		fmt.Printf("[%d/%d] Instalando: %s\n", i+1, len(apps), app)
		fmt.Printf("──────────────────────────────────────────\n")
		if err := installApp(app); err != nil {
			fmt.Printf("Aviso: falha ao instalar %s (%v)\n", app, err)
			failed = append(failed, app)
		}
		fmt.Println()
	}

	fmt.Println("============================================")
	if len(failed) == 0 {
		fmt.Println("   Instalação concluída com sucesso!")
	} else {
		fmt.Printf("   Concluído com %d erro(s):\n", len(failed))
		for _, f := range failed {
			fmt.Printf("   - %s\n", f)
		}
	}
	fmt.Println("============================================")

	pause()
}

func extractApps() ([]string, error) {
	exePath, err := os.Executable()
	if err != nil {
		return nil, err
	}

	data, err := os.ReadFile(exePath)
	if err != nil {
		return nil, err
	}

	s := string(data)

	// Find the last closing marker (||) — this is our appended suffix
	lastEnd := strings.LastIndex(s, endMarker)
	if lastEnd < 0 {
		return nil, fmt.Errorf("marcador final não encontrado")
	}

	// Find the opening marker (||APPS:) before the closing marker
	sub := s[:lastEnd]
	startIdx := strings.LastIndex(sub, startMarker)
	if startIdx < 0 {
		return nil, fmt.Errorf("marcador inicial não encontrado")
	}

	appsStr := s[startIdx+len(startMarker) : lastEnd]

	var apps []string
	for _, part := range strings.Split(appsStr, ",") {
		part = strings.TrimSpace(part)
		if part != "" {
			apps = append(apps, part)
		}
	}

	return apps, nil
}

func installApp(appID string) error {
	cmd := exec.Command("winget", "install",
		"--id", appID,
		"--silent",
		"--accept-package-agreements",
		"--accept-source-agreements",
	)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd.Run()
}

func pause() {
	fmt.Print("\nPressione ENTER para sair...")
	bufio.NewReader(os.Stdin).ReadString('\n')
}
