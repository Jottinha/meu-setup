# MeuSetup

Gerador de instaladores automáticos para Windows, inspirado no [Ninite](https://ninite.com/). Você acessa o site, marca os programas que quer, clica em "Baixar Instalador" e recebe um `.exe` que instala tudo silenciosamente usando o **Winget** — sem cliques, sem "próximo, próximo, concluir".

---

## Como funciona

O fluxo completo tem três etapas:

```
[Usuário no site] → marca checkboxes → clica em baixar
      ↓
[Spring Boot] → lê base.exe + anexa lista de IDs → devolve Instalador.exe
      ↓
[Usuário executa Instalador.exe] → pede UAC → chama winget para cada app
```

A "mágica" está no fato de que o Windows ignora texto colado no final de um `.exe`. O backend aproveita isso para personalizar o binário em tempo real sem recompilar nada: ele pega o `base.exe` fixo, gruda a lista de IDs no final e manda o arquivo resultante para o navegador. Quando o usuário executa esse arquivo, o programa Go lê os próprios bytes de trás para frente, extrai a lista e sai instalando.

---

## Arquitetura

O projeto tem três componentes de código e uma camada de infraestrutura:

### 1. `go-base/` — O executável inteligente (Go)

É compilado **uma única vez** pelo desenvolvedor. Ao ser executado na máquina do usuário ele:

1. Lê o próprio arquivo binário do disco.
2. Procura, a partir do final, pela string `||APPS:id1,id2,...||`.
3. Extrai os IDs e roda `winget install --id <ID> --silent --accept-package-agreements --accept-source-agreements` para cada um.
4. Mostra o progresso no console e aguarda ENTER ao terminar.

O binário também carrega um manifesto XML do Windows (`main.manifest`) que força o prompt de UAC (permissão de Administrador) assim que é aberto — necessário para que o Winget consiga instalar os programas corretamente.

### 2. `backend/` — A API que monta o arquivo (Spring Boot / Java 21)

Expõe um único endpoint:

```
POST /api/generate
Content-Type: application/json

["Google.Chrome", "VideoLAN.VLC", "7zip.7zip"]
```

Ao receber a requisição, ele:

1. Carrega `base.exe` do classpath (`src/main/resources/`).
2. Forma a string `||APPS:Google.Chrome,VideoLAN.VLC,7zip.7zip||` em UTF-8.
3. Concatena os bytes do executável com os bytes dessa string.
4. Retorna o resultado como `application/octet-stream` com o nome `Instalador.exe`.

O mesmo servidor Tomcat embutido serve o frontend estático na raiz (`/`).

### 3. `backend/src/main/resources/static/` — A interface web (HTML + CSS + JS)

Página de tema escuro com checkboxes organizados por categoria (Navegadores, Desenvolvimento, Produtividade, Mídia, Comunicação, Gaming, Sistema). Ao clicar em "Baixar Instalador", o JavaScript:

1. Coleta os valores dos checkboxes marcados.
2. Faz um `fetch` POST para `/api/generate` com o array de IDs.
3. Recebe a resposta como `Blob`.
4. Cria uma URL temporária com `URL.createObjectURL` e dispara o download via âncora invisível.
5. Revoga a URL para liberar memória.

### 4. `Dockerfile` — Deploy em produção (multi-stage)

Um único arquivo na raiz resolve o build completo sem nenhuma dependência pré-instalada no servidor:

- **Stage 1:** Compila o `base.exe` para Windows x64 dentro de uma imagem Go, usando cross-compilation.
- **Stage 2:** Copia o `base.exe` para os resources e compila o JAR com Maven.
- **Stage 3:** Imagem final mínima (`eclipse-temurin:21-jre-alpine`) com apenas o JAR.

---

## Estrutura de pastas

```
MeuSetup/
│
├── Dockerfile                          # Build multi-stage para produção
├── .dockerignore
│
├── go-base/                            # Componente Go (executável base)
│   ├── main.go                         # Lógica principal: self-read + winget
│   ├── main.manifest                   # Manifesto UAC (requireAdministrator)
│   ├── winres.json                     # Configuração do go-winres
│   ├── go.mod
│   └── build.ps1                       # Script de build local (Windows)
│
└── backend/                            # Componente Java (API + frontend)
    ├── pom.xml                         # Dependências Maven (Spring Boot 3.3)
    ├── Dockerfile                      # Dockerfile simples (uso local)
    └── src/main/
        ├── java/com/meusetup/
        │   ├── MeuSetupApplication.java
        │   └── controller/
        │       └── InstallerController.java   # POST /api/generate
        └── resources/
            ├── application.properties
            ├── base.exe                # Gerado pelo go-base/build.ps1 (não versionar)
            └── static/                # Frontend servido pelo Tomcat
                ├── index.html
                ├── style.css
                └── app.js
```

> **Atenção:** `backend/src/main/resources/base.exe` não existe no repositório — ele é gerado localmente pelo script de build do Go. Veja o passo a passo abaixo.

---

## Pré-requisitos

### Opção A — Rodar com Docker (recomendado)

Instale apenas o [Docker Desktop](https://www.docker.com/products/docker-desktop/). Ele inclui o Docker Compose e resolve o build completo (Go + Java) dentro dos containers — sem precisar instalar Go, Java ou Maven na sua máquina.

```powershell
winget install --id Docker.DockerDesktop
```

### Opção B — Rodar localmente sem Docker

| Ferramenta | Versão mínima | Instalar |
|---|---|---|
| [Go](https://go.dev/dl/) | 1.22 | `winget install --id GoLang.Go` |
| [Java JDK](https://adoptium.net/) | 21 | `winget install --id Oracle.JDK.21` |
| Maven | 3.9 | Não está no Winget — baixe o zip em [maven.apache.org](https://maven.apache.org/download.cgi), extraia e adicione `bin/` ao PATH |
| [Git](https://git-scm.com/) | qualquer | `winget install --id Git.Git` |

Verifique se estão instalados:

```powershell
go version
java -version
mvn -version
```

---

## Rodando localmente — passo a passo

### Passo 1 — Clone o repositório

```powershell
git clone <url-do-repositorio>
cd MeuSetup
```

### Passo 2 — Compile o executável Go

Entre na pasta `go-base` e rode o script de build. Ele instala o `go-winres` automaticamente se ainda não estiver na sua máquina, gera o manifesto UAC, compila o `.exe` e o copia para o lugar certo dentro do backend.

```powershell
cd go-base
.\build.ps1
```

Se tudo correr bem, você verá:

```
=== Build do MeuSetup Base Installer ===
Gerando recursos Windows (manifesto UAC)...
Compilando executável Windows x64...
Copiando base.exe para ..\backend\src\main\resources\base.exe ...

Build concluído! base.exe gerado e copiado.
```

> **Por que precisa do go-winres?** O compilador Go não embute manifestos UAC nativamente. O `go-winres` gera um arquivo `.syso` que o Go coleta automaticamente durante o build, injetando o manifesto no `.exe` final — sem ele, o instalador não pedirá permissão de Administrador.

### Passo 3 — Inicie o servidor Spring Boot

Volte para a raiz do projeto e entre na pasta `backend`:

```powershell
cd ..\backend
mvn spring-boot:run
```

O Maven vai baixar as dependências na primeira vez (pode demorar alguns minutos). Aguarde até ver:

```
Started MeuSetupApplication in X.XXX seconds
```

### Passo 4 — Acesse o site

Abra o navegador e acesse:

```
http://localhost:8080
```

Marque os programas, clique em **"Baixar Instalador"** e execute o `.exe` baixado como Administrador.

---

## Rodando com Docker Compose (recomendado)

O jeito mais simples de rodar o projeto. Não precisa de Go, Java nem Maven instalados — o Docker cuida do build completo.

Na raiz do projeto:

```powershell
docker compose up --build
```

Aguarde o build (alguns minutos na primeira vez — Go e Maven baixam dependências). Quando aparecer:

```
Tomcat started on port 8080 (http) with context path '/'
```

Acesse **http://localhost:8080**.

Para parar:

```powershell
docker compose down
```

> **Nas próximas vezes**, se o código não mudou, o Docker usa cache e sobe em segundos:
> ```powershell
> docker compose up
> ```

---

## Deploy em produção

Para Railway, Render, Fly.io ou qualquer VPS, basta apontar o repositório — a plataforma detecta o `Dockerfile` na raiz e faz o build automaticamente. Não é necessário nenhuma configuração adicional.

---

## Adicionando novos programas

Para adicionar um app à lista do site, edite `backend/src/main/resources/static/index.html` e adicione um novo card na categoria adequada. O `value` do checkbox deve ser o **ID exato do Winget**.

```html
<label class="app-card">
    <input type="checkbox" value="Publisher.AppName">
    <div class="app-info">
        <span class="app-name">Nome do App</span>
        <span class="app-id">Publisher.AppName</span>
    </div>
</label>
```

Para descobrir o ID correto de um programa, rode no terminal do Windows:

```powershell
winget search "nome do programa"
```

---

## Como o truque binário funciona

O Windows carrega um `.exe` lendo apenas os cabeçalhos PE (Portable Executable) no início do arquivo e ignora completamente qualquer dado após o fim lógico do programa. Isso significa que é seguro colar texto arbitrário depois dos bytes do executável — o sistema operacional simplesmente o ignora na hora de rodar.

O backend explora isso assim:

```
[bytes do base.exe] + [||APPS:Google.Chrome,VideoLAN.VLC||]
         ↑                              ↑
  executável intacto             texto ignorado pelo Windows,
  (roda normalmente)             lido pelo próprio programa Go
```

O programa Go, ao iniciar, lê o próprio arquivo do disco com `os.ReadFile`, procura a última ocorrência de `||APPS:` e extrai tudo até o `||` seguinte. Como a busca é feita de trás para frente com `strings.LastIndex`, ela sempre encontrará a lista correta no sufixo, independente do conteúdo binário do executável em si.
