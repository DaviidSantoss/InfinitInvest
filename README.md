# InfinitInvest

Aplicativo desktop de **gerenciamento financeiro e de investimentos**, desenvolvido em **Java** com **JavaFX**, seguindo o padrão **MVC**, autenticação com **verificação por e-mail**, integração com **APIs de mercado financeiro** e armazenamento local via **SQLite**.

O projeto está funcional e finalizado em sua primeira versão estável, sendo utilizado como **portfólio** e também para **uso pessoal**.

---

## 🚀 Objetivo do Projeto

O **InfinitInvest** nasceu da necessidade de ter uma ferramenta mais flexível e transparente para acompanhamento de investimentos, sem depender exclusivamente de plataformas fechadas.

O foco do projeto é:

* Consolidar dados de investimentos em um único lugar
* Exercitar boas práticas de arquitetura e organização de código
* Criar uma base sólida para futuras evoluções

---

## ✨ Funcionalidades Implementadas

* Cadastro de usuário com nome, e-mail e senha
* Verificação de e-mail via código enviado automaticamente
* Login seguro com opção **"Manter conectado"**
* Persistência de dados local utilizando **SQLite**
* Interface gráfica moderna em **JavaFX**
* Arquitetura **MVC (Model–View–Controller)**

### 📊 Gestão de Investimentos

* Cadastro e controle de ativos (ações, FIIs, ETFs, entre outros)
* Registro de preço pago, preço médio e quantidade de cotas
* Distribuição do patrimônio por categoria e por ativo
* Visão consolidada do patrimônio total

### 👤 Experiência do Usuário

* Perfil do usuário com foto
* Aba de anotações para registro de ideias, lembretes e observações financeiras

---

## 🛠 Tecnologias e Bibliotecas Utilizadas

**Linguagem:**

* Java 24

**Interface & Persistência:**

* JavaFX
* SQLite

**Arquitetura:**

* MVC (Model–View–Controller)

**APIs Externas:**

* EODHD (dados financeiros)
* Brapi (dados do mercado brasileiro)
* LogoKit (logos de ativos)

**Principais Dependências:**

* angus-mail-2.0.3
* jakarta-activation-api-2.1.3
* jakarta.mail-api-2.1.3
* sqlite-jdbc-3.50.2.0
* json.20231013.richtext
* Módulos JavaFX (controls, fxml, graphics, etc.)

---

## 🔑 Configurações Obrigatórias (APIs e Serviços)

Para executar o projeto corretamente, é necessário configurar **tokens e caminhos locais** nas classes abaixo:

### 📊 Classe `Brapi`

Configure os tokens das APIs financeiras:

```java
private static final String EODHD_TOKEN = "SEU_TOKEN_EODHD";
private static final String TOKEN = "SEU_TOKEN_BRAPI";
```

---

### 🖼 Classe `LogoKit`

Configure o token da API de logos:

```java
private static final String TOKEN = "SEU_TOKEN_LOGOKIT";
```

---

### 🏦 Classe `TesouroDataLoader`

Informe o caminho do arquivo CSV na sua máquina:

```java
private static final Path ARQUIVO_CSV = Paths.get("C:/SEU_CAMINHO/arquivo.csv");
```

---

### ✉️ Classe `CodigoVerific`

Para que a verificação por e-mail funcione, é necessário configurar um e-mail com **senha de aplicativo** (recomendado: Zoho Mail):

```java
final String remetente = "SEU_EMAIL";
final String senhaApp = "SENHA_DE_APLICATIVO";
```

---

## 📦 Como Executar o Projeto

### 1️⃣ Pré-requisitos

* Java 24 (JDK e JRE)
* JavaFX SDK configurado
* Driver SQLite no classpath
* Conexão com a internet (para envio de e-mails e consumo de APIs)

---

### 2️⃣ Executando no Eclipse

1. Importe o projeto no Eclipse
2. Adicione todas as bibliotecas ao **Modulepath**
3. Configure as opções de execução do JavaFX:

```bash
--module-path "caminho/do/javafx-sdk/lib" --add-modules javafx.controls,javafx.fxml
```

4. Execute a classe principal (`MainApp`)

---

## 📌 Observações Importantes

* O envio de códigos por e-mail só funcionará após a configuração correta do e-mail e da senha de aplicativo
* As APIs externas exigem tokens válidos
* O projeto foi desenvolvido para ambiente desktop
* Em sistemas sem aceleração gráfica adequada, o JavaFX pode exigir ajustes adicionais

### 📈 Sobre o cálculo de rendimento mensal

O rendimento mensal exibido no sistema é uma **estimativa** baseada nos dividendos dos últimos 12 meses.

O cálculo funciona da seguinte forma:

* Soma-se o total de dividendos pagos pelo ativo nos últimos 12 meses
* Esse valor é dividido por 12, obtendo uma média mensal

Exemplo:

* Se um ativo possui **15% de Dividend Yield ao ano**, o rendimento mensal estimado será de aproximadamente **1,25% ao mês**, sempre em relação ao valor investido.

---

## 🔮 Próximas Evoluções Planejadas

* Cálculo de rentabilidade ponderada da carteira
* Cálculo aproximado do rendimento mensal dos investimentos

---

## 👤 Autor

**David**
Desenvolvedor e investidor, apaixonado por programação, economia e soluções inovadoras.
