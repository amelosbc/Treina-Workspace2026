# Sample Ruby on Rails + MongoDB (WSL2)

## Visao Geral do Ambiente

```
Desktop Windows
|
+-- Docker Desktop
|   +-- Container mongo:7.0 (porta 27017)
|
+-- WSL2 Ubuntu
    +-- rbenv -> Ruby 3.4.9
    +-- gem rails 8.1.3
    +-- gem mongoid 9.0.10
    +-- gem rspec-rails 8.0.4
    +-- gem rubocop 1.86.0
    +-- VSCode (extensoes: Ruby LSP, Rails, RuboCop)
            +-- Projeto: dimensa_api/
                +-- app/models/      -> Mongoid Documents
                +-- app/controllers/ -> API REST (JSON)
                +-- spec/            -> RSpec
                +-- config/mongoid.yml -> aponta pro Docker
```

---
gem install rails -v 8.1.3
gem install rubocop -v 1.86.0
rbenv rehash
rails --version

## PASSO 1 - Instalar o rbenv no WSL2/Ubuntu

### 1.1 Instalar dependencias de compilacao

```bash
sudo apt update && sudo apt install -y \
  build-essential libssl-dev libreadline-dev zlib1g-dev \
  libyaml-dev libffi-dev libgdbm-dev libncurses5-dev \
  autoconf bison curl git
```

### 1.2 Instalar rbenv + ruby-build

```bash
# Clonar rbenv
git clone https://github.com/rbenv/rbenv.git ~/.rbenv

# Clonar ruby-build como plugin
git clone https://github.com/rbenv/ruby-build.git ~/.rbenv/plugins/ruby-build

# Adicionar ao PATH (bash)
echo '' >> ~/.bashrc
echo '# rbenv' >> ~/.bashrc
echo 'export PATH="$HOME/.rbenv/bin:$PATH"' >> ~/.bashrc
echo 'eval "$(rbenv init - bash)"' >> ~/.bashrc

# Carregar no shell atual
source ~/.bashrc
```

### 1.3 Verificar instalacao

```bash
rbenv --version
# rbenv 1.x.x

ruby-build --definitions | tail -5
# Deve listar versoes disponiveis
```

---

## PASSO 2 - Instalar Ruby 3.4.9

### 2.1 Instalar e definir como padrao

```bash
# Instalar (pode levar alguns minutos - compila do fonte)
rbenv install 3.4.9

# Definir como versao global
rbenv global 3.4.9

# Recarregar shims
rbenv rehash
```

### 2.2 Verificar

```bash
ruby --version
# ruby 3.4.9 ...

which ruby
# /home/<user>/.rbenv/shims/ruby

gem --version
# 3.x.x
```

---

## PASSO 3 - Instalar Rails e Gems principais

### 3.1 Instalar Rails

```bash
gem install rails -v 8.1.3
rbenv rehash

rails --version
# Rails 8.1.3
```

### 3.2 Instalar RuboCop globalmente (opcional, para uso no terminal)

```bash
gem install rubocop -v 1.86.0
rbenv rehash

rubocop --version
# 1.86.0
```

> As demais gems (mongoid, rspec-rails, puma) serao instaladas via Gemfile do projeto no Passo 6.

---

## PASSO 4 - MongoDB via Docker

### 4.1 Verificar Docker Desktop

No Windows, certifique-se de que o **Docker Desktop** esta rodando com a integracao WSL2 ativada.

```bash
docker --version
# Docker version 2x.x.x
```

Caso o comando acima nao funcione dentro do WSL2:
1. Abrir **Docker Desktop** no Windows
2. Ir em **Settings > Resources > WSL Integration**
3. Ativar a integracao com a sua distro Ubuntu
4. Clicar **Apply & Restart**
5. Fechar e reabrir o terminal WSL2

### 4.2 Baixar a imagem do MongoDB 7.0

```bash
# Baixar a imagem oficial do MongoDB
docker pull mongo:7.0

# Verificar que a imagem foi baixada
docker images | grep mongo
# mongo   7.0   xxxxxxxxxxxx   xxx MB
```

> Isso faz download de ~700MB do Docker Hub. So precisa ser feito uma vez.

### 4.3 Criar e subir o container

```bash
docker run -d \
  --name mongodb \
  -p 27017:27017 \
  -v mongodb_data:/data/db \
  mongo:7.0
```

O que cada flag faz:
| Flag | Funcao |
|------|--------|
| `-d` | Roda em background (detached) |
| `--name mongodb` | Nome do container para referencia |
| `-p 27017:27017` | Expoe a porta do Mongo para o WSL2/localhost |
| `-v mongodb_data:/data/db` | Volume persistente (dados sobrevivem a reinicio do container) |
| `mongo:7.0` | Imagem que acabamos de baixar |

### 4.4 Verificar

```bash
# Ver container rodando
docker ps
# CONTAINER ID  IMAGE     ...  PORTS                     NAMES
# xxxxxxxxxxxx  mongo:7.0 ...  0.0.0.0:27017->27017/tcp  mongodb

# Testar conexao
docker exec -it mongodb mongosh --eval "db.runCommand({ping:1})"
# { ok: 1 }
```

Se o container parou por algum motivo:
```bash
# Ver todos os containers (inclusive parados)
docker ps -a

# Ver logs para diagnosticar
docker logs mongodb
```

### 4.5 Comandos uteis do container

```bash
# Parar
docker stop mongodb

# Iniciar novamente
docker start mongodb

# Ver logs
docker logs mongodb

# Remover container (dados persistem no volume)
docker rm mongodb

# Recriar do zero (se necessario)
docker run -d --name mongodb -p 27017:27017 -v mongodb_data:/data/db mongo:7.0

# Acessar o shell do MongoDB
docker exec -it mongodb mongosh
```

---

## PASSO 5 - Configurar o VSCode para Ruby

### 5.1 Extensoes recomendadas

Abrir VSCode e instalar:

| Extensao | ID |
|---|---|
| Ruby LSP | `Shopify.ruby-lsp` |
| Rails | `bung.rails` |
| RuboCop | `rubocop.vscode-rubocop` |
| endwise | `kaiwood.endwise` |

```bash
# Ou via terminal:
code --install-extension Shopify.ruby-lsp
code --install-extension bung.rails
code --install-extension rubocop.vscode-rubocop
code --install-extension kaiwood.endwise
```

### 5.2 Settings recomendadas (.vscode/settings.json)

```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "Shopify.ruby-lsp",
  "[ruby]": {
    "editor.defaultFormatter": "Shopify.ruby-lsp",
    "editor.tabSize": 2
  },
  "rubyLsp.formatter": "rubocop"
}
```

---

## PASSO 6 - Criar a Aplicacao Pratica

### 6.1 Gerar o projeto Rails (API mode, sem ActiveRecord)

```bash
cd ~/workspaces2026/Treina-Workspace2026/sample-ruby

rails new dimensa_api --api --skip-active-record --skip-test
cd dimensa_api
```%sudo ALL=(ALL) NOPASSWD: ALL

> `--api` gera uma app Rails enxuta para APIs JSON.
> `--skip-active-record` remove o ORM SQL (usaremos Mongoid).
> `--skip-test` pula Minitest (usaremos RSpec).

### 6.2 Configurar Gemfile

Editar `Gemfile` e adicionar/ajustar:

```ruby
source "https://rubygems.org"

ruby "3.4.9"

gem "rails", "~> 8.1.3"
gem "puma", "~> 7.2.0"
gem "mongoid", "~> 9.0.10"

# Build JSON APIs
gem "jbuilder"

group :development, :test do
  gem "rspec-rails", "~> 8.0.4"
  gem "debug", platforms: %i[mri windows], require: "debug/prelude"
end

group :development do
  gem "rubocop", "~> 1.86.0", require: false
  gem "rubocop-rails", require: false
end
```

### 6.3 Instalar dependencias

```bash
bundle install
```

### 6.4 Configurar Mongoid

```bash
rails g mongoid:config
```

Isso gera `config/mongoid.yml`. Verificar que aponta para localhost:

```yaml
# config/mongoid.yml
development:
  clients:
    default:
      database: dimensa_api_development
      hosts:
        - localhost:27017
      options:
        server_selection_timeout: 1

test:
  clients:
    default:
      database: dimensa_api_test
      hosts:
        - localhost:27017
      options:
        server_selection_timeout: 1
```

### 6.5 Instalar RSpec

```bash
rails generate rspec:install
```

Isso cria:
- `.rspec`
- `spec/spec_helper.rb`
- `spec/rails_helper.rb`

### 6.6 Configurar RuboCop

Criar `.rubocop.yml` na raiz do projeto:

```yaml
require:
  - rubocop-rails

AllCops:
  NewCops: enable
  TargetRubyVersion: 3.4
  Exclude:
    - "bin/**/*"
    - "config/**/*"
    - "db/**/*"
    - "vendor/**/*"

Style/Documentation:
  Enabled: false

Metrics/BlockLength:
  Exclude:
    - "spec/**/*"
```

### 6.7 Verificar que tudo funciona

```bash
# Servidor
rails server
# => Booting Puma
# => Rails 8.1.3 application starting in development
# => Listening on http://127.0.0.1:3000
# Ctrl+C para parar

# RSpec
bundle exec rspec
# 0 examples, 0 failures

# RuboCop
bundle exec rubocop
```

---

## PASSO 7 - Primeira Feature: CRUD de Clientes (API REST)

### 7.1 Criar o Model (Mongoid Document)

```bash
rails g model Cliente nome:string email:string telefone:string documento:string
```

Editar `app/models/cliente.rb`:

```ruby
class Cliente
  include Mongoid::Document
  include Mongoid::Timestamps

  field :nome, type: String
  field :email, type: String
  field :telefone, type: String
  field :documento, type: String

  validates :nome, presence: true
  validates :email, presence: true, uniqueness: true
  validates :documento, presence: true, uniqueness: true
end
```

### 7.2 Criar o Controller

```bash
rails g controller api/v1/clientes --no-helper --no-assets
```

Editar `app/controllers/api/v1/clientes_controller.rb`:

```ruby
module Api
  module V1
    class ClientesController < ApplicationController
      before_action :set_cliente, only: %i[show update destroy]

      # GET /api/v1/clientes
      def index
        @clientes = Cliente.all
        render json: @clientes
      end

      # GET /api/v1/clientes/:id
      def show
        render json: @cliente
      end

      # POST /api/v1/clientes
      def create
        @cliente = Cliente.new(cliente_params)

        if @cliente.save
          render json: @cliente, status: :created
        else
          render json: { errors: @cliente.errors.full_messages }, status: :unprocessable_entity
        end
      end

      # PATCH/PUT /api/v1/clientes/:id
      def update
        if @cliente.update(cliente_params)
          render json: @cliente
        else
          render json: { errors: @cliente.errors.full_messages }, status: :unprocessable_entity
        end
      end

      # DELETE /api/v1/clientes/:id
      def destroy
        @cliente.destroy
        head :no_content
      end

      private

      def set_cliente
        @cliente = Cliente.find(params[:id])
      end

      def cliente_params
        params.require(:cliente).permit(:nome, :email, :telefone, :documento)
      end
    end
  end
end
```

### 7.3 Configurar Rotas

Editar `config/routes.rb`:

```ruby
Rails.application.routes.draw do
  namespace :api do
    namespace :v1 do
      resources :clientes
    end
  end

  get "up" => "rails/health#show", as: :rails_health_check
end
```

### 7.4 Testar com curl

```bash
# Iniciar servidor
rails server &

# Criar cliente
curl -s -X POST http://localhost:3000/api/v1/clientes \
  -H "Content-Type: application/json" \
  -d '{"cliente":{"nome":"Joao Silva","email":"joao@email.com","telefone":"11999999999","documento":"12345678900"}}' | jq

# Listar clientes
curl -s http://localhost:3000/api/v1/clientes | jq

# Buscar por ID (substituir <id> pelo _id retornado)
curl -s http://localhost:3000/api/v1/clientes/<id> | jq

# Atualizar
curl -s -X PATCH http://localhost:3000/api/v1/clientes/<id> \
  -H "Content-Type: application/json" \
  -d '{"cliente":{"telefone":"11888888888"}}' | jq

# Deletar
curl -s -X DELETE http://localhost:3000/api/v1/clientes/<id> -w "\nHTTP %{http_code}\n"
```

---

## PASSO 8 - Escrever seu primeiro teste RSpec

### 8.1 Criar spec do Model

Criar `spec/models/cliente_spec.rb`:

```ruby
require "rails_helper"

RSpec.describe Cliente, type: :model do
  describe "validations" do
    it "is valid with all required attributes" do
      cliente = Cliente.new(
        nome: "Maria Santos",
        email: "maria@email.com",
        documento: "98765432100"
      )
      expect(cliente).to be_valid
    end

    it "is invalid without nome" do
      cliente = Cliente.new(nome: nil)
      expect(cliente).not_to be_valid
      expect(cliente.errors[:nome]).to include("can't be blank")
    end

    it "is invalid without email" do
      cliente = Cliente.new(email: nil)
      expect(cliente).not_to be_valid
      expect(cliente.errors[:email]).to include("can't be blank")
    end

    it "is invalid without documento" do
      cliente = Cliente.new(documento: nil)
      expect(cliente).not_to be_valid
      expect(cliente.errors[:documento]).to include("can't be blank")
    end

    it "is invalid with duplicate email" do
      Cliente.create!(nome: "A", email: "dup@email.com", documento: "111")
      duplicate = Cliente.new(nome: "B", email: "dup@email.com", documento: "222")
      expect(duplicate).not_to be_valid
    end

    it "is invalid with duplicate documento" do
      Cliente.create!(nome: "A", email: "a@email.com", documento: "111")
      duplicate = Cliente.new(nome: "B", email: "b@email.com", documento: "111")
      expect(duplicate).not_to be_valid
    end
  end
end
```

### 8.2 Criar spec do Controller (Request spec)

Criar `spec/requests/api/v1/clientes_spec.rb`:

```ruby
require "rails_helper"

RSpec.describe "Api::V1::Clientes", type: :request do
  let(:valid_attributes) do
    { nome: "Joao Silva", email: "joao@email.com", telefone: "11999999999", documento: "12345678900" }
  end

  before { Cliente.delete_all }

  describe "GET /api/v1/clientes" do
    it "returns all clientes" do
      Cliente.create!(valid_attributes)

      get "/api/v1/clientes"

      expect(response).to have_http_status(:ok)
      expect(JSON.parse(response.body).size).to eq(1)
    end
  end

  describe "POST /api/v1/clientes" do
    it "creates a new cliente" do
      expect {
        post "/api/v1/clientes", params: { cliente: valid_attributes }
      }.to change(Cliente, :count).by(1)

      expect(response).to have_http_status(:created)
    end

    it "returns errors for invalid data" do
      post "/api/v1/clientes", params: { cliente: { nome: "" } }

      expect(response).to have_http_status(:unprocessable_entity)
      expect(JSON.parse(response.body)).to have_key("errors")
    end
  end

  describe "PATCH /api/v1/clientes/:id" do
    it "updates the cliente" do
      cliente = Cliente.create!(valid_attributes)

      patch "/api/v1/clientes/#{cliente.id}", params: { cliente: { nome: "Novo Nome" } }

      expect(response).to have_http_status(:ok)
      expect(cliente.reload.nome).to eq("Novo Nome")
    end
  end

  describe "DELETE /api/v1/clientes/:id" do
    it "deletes the cliente" do
      cliente = Cliente.create!(valid_attributes)

      expect {
        delete "/api/v1/clientes/#{cliente.id}"
      }.to change(Cliente, :count).by(-1)

      expect(response).to have_http_status(:no_content)
    end
  end
end
```

### 8.3 Configurar limpeza do banco de teste

Adicionar ao `spec/rails_helper.rb`:

```ruby
RSpec.configure do |config|
  # Limpar MongoDB antes de cada teste
  config.before(:each) do
    Mongoid.purge!
  end
end
```

### 8.4 Rodar os testes

```bash
# Todos os testes
bundle exec rspec

# Apenas model
bundle exec rspec spec/models/

# Apenas requests
bundle exec rspec spec/requests/

# Com output detalhado
bundle exec rspec --format documentation
```

Saida esperada:

```
Cliente
  validations
    is valid with all required attributes
    is invalid without nome
    is invalid without email
    is invalid without documento
    is invalid with duplicate email
    is invalid with duplicate documento

Api::V1::Clientes
  GET /api/v1/clientes
    returns all clientes
  POST /api/v1/clientes
    creates a new cliente
    returns errors for invalid data
  PATCH /api/v1/clientes/:id
    updates the cliente
  DELETE /api/v1/clientes/:id
    deletes the cliente

11 examples, 0 failures
```

---

## Comandos do Dia a Dia

```bash
# Iniciar MongoDB
docker start mongodb

# Iniciar servidor Rails
cd dimensa_api && rails server

# Rodar testes
bundle exec rspec

# Rodar linter
bundle exec rubocop

# Corrigir lint automaticamente
bundle exec rubocop -A

# Console Rails (interativo)
rails console

# Ver rotas
rails routes
```
