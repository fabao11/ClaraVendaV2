# Venda Clara

Aplicação Android nativa (Kotlin + Jetpack Compose) para preparar anúncios e gerir vendas.

**Estado: Etapa 2.** Fotografias, rascunhos guardados e geração do anúncio por IA (título, descrição, detalhes a confirmar). Ainda **não** existem medidas, assistente de mensagens nem margens.

A app nunca publica nada na Vinted, nunca envia mensagens e nunca pede dados de acesso da Vinted: tu revês e copias tudo.

## Custos (lê antes de ativar a IA)
- **Anthropic (IA):** pago por utilização, consoante as fotografias analisadas. Consulta os preços atuais antes de começar e define um **limite mensal de gastos** na conta.
- **Cloudflare (servidor):** o plano gratuito chega para uso pessoal; confirma os limites atuais.
- Compilar e instalar a app não tem custos.

## Como funciona a segurança
- A chave da IA fica **só no servidor** (segredo do Cloudflare), nunca no APK nem no GitHub.
- Quem usa a app precisa de um **código de acesso** que tu defines. Cada pessoa vê apenas os artigos guardados no seu telemóvel.
- As fotografias só são enviadas depois do consentimento. São reduzidas e enviadas sem localização. O servidor não as guarda.

## A. Android Studio e telemóvel (Windows)
1. Instala o Android Studio: https://developer.android.com/studio (aceita as opções por defeito).
2. **File > Open** e escolhe a pasta `VendaClara`. Espera que o **Gradle sync** termine (vários minutos, com internet).
3. No telemóvel: **Definições > Acerca do telefone**, toca 7 vezes em **Número de compilação**; depois ativa **Depuração USB** em **Opções de programador**.
4. Liga o telemóvel por USB, autoriza no ecrã e carrega no botão verde ▶ no Android Studio.
5. Para gerar o APK: **Build > Build Bundle(s) / APK(s) > Build APK(s)** e depois **locate**. Ficheiro: `app/build/outputs/apk/debug/app-debug.apk`.

## B. Ligar a IA (só uma vez, por quem gere o servidor)
1. **Instalar:** Node.js (versão LTS) em https://nodejs.org.
2. **Criar conta (grátis):** https://dash.cloudflare.com/sign-up.
3. **Criar conta e autorizar custos:** https://console.anthropic.com. Adiciona crédito (**isto tem custos**), define um limite mensal e cria uma **chave de API**. Guarda-a; não a partilhes nem a envies para o GitHub.
4. Abre o **PowerShell** na pasta `VendaClara\worker` (no Explorador: clica na barra do caminho, escreve `powershell` e Enter).
5. Executa, um comando de cada vez:
   - `npx wrangler login` — abre o browser; carrega em **Allow**.
   - `npx wrangler secret put ANTHROPIC_API_KEY` — cola a chave quando pedir.
   - `npx wrangler secret put CODIGOS_ACESSO` — escreve um ou mais códigos separados por vírgula, por exemplo `codigo-pai-4821,codigo-joao-7730` (inventa os teus; sem espaços).
   - `npx wrangler deploy` — no fim mostra o endereço `https://venda-clara.….workers.dev`.
6. **No telemóvel:** abre a app, toca na roda dentada, cola o endereço e o código, carrega em **Testar ligação**. Deve aparecer «Ligação estabelecida com sucesso.»
7. Adiciona fotografias a um artigo. Na primeira vez aparece o pedido de consentimento; ao aceitar, a IA prepara o anúncio.

## C. GitHub (compilação automática)
1. Cria conta em https://github.com e um repositório **Private**.
2. Envia os ficheiros (por exemplo com https://desktop.github.com).
3. Separador **Actions**: corre o fluxo **Compilar APK**. No fim, transfere `venda-clara-debug-apk` em **Artifacts**.
4. Opcional: em **Settings > Secrets and variables > Actions > Variables**, cria `SERVIDOR_URL` com o endereço do servidor. O APK já vem com o endereço preenchido (não é segredo; o código de acesso continua a ser escrito em cada telemóvel).

Nunca coloques a chave da IA nem códigos de acesso em ficheiros do repositório.

## Estrutura
- `app/…/dados` – base de dados local (Room, versão 2 com migração) e repositório.
- `app/…/ia` – definições, modelos e comunicação com o servidor.
- `app/…/ui` – ecrãs (Compose) e ViewModels.
- `worker/` – servidor de IA (Cloudflare Worker).
- `.github/workflows/compilar.yml` – compilação automática do APK de debug.
