# Volaris Finanças

O **Volaris Finanças** é um aplicativo Android moderno para gerenciamento de finanças pessoais. Desenvolvido com as melhores práticas da plataforma, ele permite que você tenha total controle sobre seu fluxo de caixa, defina orçamentos, crie metas de economia e receba alertas de contas a pagar, tudo em uma interface limpa, intuitiva e fluida.

O aplicativo também conta com um recurso de análise inteligente, utilizando IA para fornecer relatórios e resumos textuais de seus gastos mensais, ajudando a identificar pontos de economia.

---

## 🚀 Funcionalidades

- 📊 **Fluxo de Caixa**: Registro simplificado de transações de entrada e saída, com categorização completa.
- 🎯 **Orçamentos por Categoria**: Planeje seus gastos mensais estipulando limites para alimentação, transporte, lazer, etc.
- 🏁 **Metas de Economia**: Defina objetivos financeiros de médio e longo prazo e acompanhe a evolução do seu progresso.
- 🔔 **Lembretes de Contas**: Nunca mais esqueça de pagar uma conta! Controle suas contas recorrentes e datas de vencimento.
- 🧠 **Resumo Inteligente**: Integração com IA para gerar insights e análises automáticas sobre seu comportamento financeiro mensal.

---

## 🛠️ Tecnologias e Arquitetura

O projeto foi estruturado seguindo os princípios de desenvolvimento Android modernos:

- **Linguagem**: [Kotlin](https://kotlinlang.org/)
- **UI**: [Jetpack Compose](https://developer.android.com/compose) (declarativo e moderno)
- **Persistência**: [Room Database](https://developer.android.com/training/data-storage/room) para armazenamento local offline
- **Assincronia**: Kotlin Coroutines e Kotlin Flow para fluxos de dados reativos
- **Arquitetura**: MVVM (Model-View-ViewModel) para separação de responsabilidades
- **Injeção/Integração de IA**: SDK do Gemini via Firebase AI para geração de insights financeiros

---

## 📦 Como Executar o Projeto

### Pré-requisitos
- [Android Studio](https://developer.android.com/studio) (versão Ladybug ou superior recomendada)
- SDK do Android instalada (API 24+)
- Uma chave de API do Gemini (opcional, necessária apenas para recursos de IA)

### Passos para execução
1. **Clonar o Repositório:**
   ```bash
   git clone https://github.com/foxredoficial/volaris-financas.git
   cd volaris-financas
   ```

2. **Configurar as Variáveis de Ambiente:**
   Crie um arquivo chamado `.env` na raiz do projeto (use o `.env.example` como base):
   ```properties
   GEMINI_API_KEY=sua_chave_de_api_aqui
   ```

3. **Abrir no Android Studio:**
   - Selecione **File > Open** no Android Studio e escolha o diretório do projeto.
   - Aguarde a sincronização do Gradle e a indexação do projeto.

4. **Rodar o Aplicativo:**
   - Conecte um dispositivo físico com depuração USB ativa ou utilize um Emulador Android.
   - Clique no botão **Run** (ícone verde de "Play") no topo do Android Studio.

---

## 📄 Licença

Este projeto está sob a licença MIT. Consulte o arquivo [LICENSE](LICENSE) para obter mais detalhes.
