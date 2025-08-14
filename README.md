# InfinitInvest

Aplicativo de gerenciamento financeiro e de investimentos com interface moderna em JavaFX, autenticação com verificação por e-mail e armazenamento local via SQLite.
Atualmente permite cadastro, login seguro e opção de manter o usuário conectado.

# 🚀 Objetivo do Projeto

O InfinitInvest nasceu como um projeto de portfólio e uso pessoal, com a proposta de oferecer funcionalidades que ainda não encontrei em softwares de gestão de investimentos.
Meu objetivo é criar uma ferramenta robusta para acompanhamento de ativos e organização financeira, sem depender de soluções fechadas.

# ✨ Funcionalidades Atuais

Cadastro de usuário com nome, e-mail e senha.

Verificação de e-mail via código enviado automaticamente ao endereço fornecido.

Armazenamento local seguro utilizando SQLite.

Login com opção "Continuar conectado" (mantém o usuário logado nas próximas aberturas do app).

Interface gráfica moderna utilizando JavaFX.

# 🛠 Tecnologias e Bibliotecas Utilizadas

Linguagem: Java 24

Frameworks/Bancos: JavaFX, SQLite

Dependências:

angus-mail-2.0.3

jakarta-activation-api-2.1.3

jakarta.mail-api-2.1.3

javafx.base

javafx.controls

javafx.fxml

javafx.graphics

javafx.media

javafx.swing

javafx.web

javafx-swt

jdk.jsobject

jfx.incubator.input

json.20231013.richtext

sqlite-jdbc-3.50.2.0

# 🔮 Funcionalidades Futuras

Cadastro e controle de ativos: ações, FIIs, ETFs, entre outros.

Registro de preço pago, preço médio e quantidade de cotas.

Distribuição do patrimônio por categoria/ativo.

Visão geral do patrimônio total.

Logout e troca de contas.

Perfil do usuário com foto e nome.

Aba de anotações estilo editor de texto para registro de ideias ou lembretes.

Entre outras coisas.

# 📦 Como Executar o Projeto

### 1-Pré-requisitos:

Java 24 instalado (JDK e JRE compatíveis).

JavaFX SDK configurado no ambiente de execução.

Driver SQLite incluído no classpath.

### 2-Configuração do envio de e-mails (obrigatória para o cadastro funcionar):
No arquivo responsável pelo envio de códigos, substitua os valores:
```java
final String remetente = "seu_email@provedor.com";
```
```java
final String senhaApp = "sua_senha_de_app";
```

### 💡 Recomendo o uso do Zoho Mail ou outro provedor que permita senha de aplicativo.

### 3-Rodando o projeto no Eclipse:

Importe o projeto.

Adicione as bibliotecas listadas acima ao Modulepath.

Configure a execução com as opções do JavaFX:

```java
--module-path "caminho/do/javafx-sdk/lib" --add-modules javafx.controls,javafx.fxml
```

Execute a classe principal (MainApp).

# 📌 Observações

O envio de código por e-mail só funcionará após configurar seu e-mail e senha no código.

Certifique-se de ter conexão com a internet para o envio de códigos.

Em sistemas sem suporte gráfico adequado, o JavaFX pode precisar de configuração extra para renderização.

# 👤 Autor

David – Desenvolvedor e investidor, apaixonado por programação, economia e soluções inovadoras.



