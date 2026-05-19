package main

import (
	"bufio"
	"fmt"
	"os"
	"os/exec"
)

func main() {
	fmt.Println("============================================")
	fmt.Println("   MeuSetup - Instalador Automatico")
	fmt.Println("============================================")
	fmt.Println()

	apps, err := readTrailer()
	if err != nil {
		fmt.Printf("Erro: %s\n", err.Error())
		fmt.Println("Gere um instalador personalizado em nosso site.")
		pause()
		os.Exit(1)
	}

	fmt.Printf("Instalando %d aplicativo(s) via Winget...\n\n", len(apps))

	failed := []string{}
	for i, app := range apps {
		fmt.Printf("------------------------------------------\n")
		fmt.Printf("[%d/%d] Instalando: %s\n", i+1, len(apps), app)
		fmt.Printf("------------------------------------------\n")
		if err := installApp(app); err != nil {
			fmt.Printf("Aviso: falha ao instalar %s (%v)\n", app, err)
			failed = append(failed, app)
		}
		fmt.Println()
	}

	fmt.Println("============================================")
	if len(failed) == 0 {
		fmt.Println("   Instalacao concluida com sucesso!")
	} else {
		fmt.Printf("   Concluido com %d erro(s):\n", len(failed))
		for _, f := range failed {
			fmt.Printf("   - %s\n", f)
		}
	}
	fmt.Println("============================================")

	pause()
}

func installApp(appID string) error {
	if !isValidAppID(appID) {
		return fmt.Errorf("ID rejeitado pela validacao local")
	}
	cmd := exec.Command("winget", "install",
		"--id", appID,
		"--exact",
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
