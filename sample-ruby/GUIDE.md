# Dimensa API - Step-by-Step Guide

Ruby on Rails API with CRUD web pages and MongoDB integration.

---

## STEP 1 - Verify Prerequisites

### 1.1 Ruby, Rails and Bundler

```bash
ruby --version       # Expected: 3.4.x
rails --version      # Expected: 8.1.x
bundler --version    # Expected: 4.x
```

### 1.2 Docker and MongoDB

```bash
docker --version
docker compose version
```

Start MongoDB if not already running:

```bash
docker compose up -d
```

Verify MongoDB is healthy:

```bash
docker exec -it mongo_dev mongosh -u admin -p admin123 --eval "db.runCommand({ ping: 1 })"
# Expected: { ok: 1 }
```

> **Note:** `mongosh` runs inside the container — no need to install it on the host.

---

## STEP 2 - Create the Rails Application

### 2.1 Generate the project

```bash
rails new dimensa_api --api --skip-active-record --skip-test
```

Flags:
- `--api` — lightweight API-only app (no views, no asset pipeline)
- `--skip-active-record` — we use Mongoid instead of ActiveRecord
- `--skip-test` — we will add RSpec manually

### 2.2 Enter the project directory

```bash
cd dimensa_api
```

---

## STEP 3 - Configure Dependencies (Gemfile)

### 3.1 Add gems

Edit `Gemfile` and add/replace the following:

```ruby
source "https://rubygems.org"

ruby "3.4.9"

gem "rails", "~> 8.1.3"
gem "puma", "~> 7.2.0"
gem "mongoid", "~> 9.0"

gem "tzinfo-data", platforms: %i[windows jruby]
gem "bootsnap", require: false

group :development, :test do
  gem "rspec-rails", "~> 8.0"
  gem "debug", platforms: %i[mri windows], require: "debug/prelude"
end

group :development do
  gem "rubocop", require: false
  gem "rubocop-rails", require: false
end
```

### 3.2 Install dependencies

```bash
bundle install
```

---

## STEP 4 - Configure MongoDB (Mongoid)

### 4.1 Generate Mongoid config

```bash
bin/rails g mongoid:config
```

### 4.2 Edit `config/mongoid.yml`

```yaml
development:
  clients:
    default:
      database: dimensa_api_development
      hosts:
        - localhost:27017
      options:
        user: admin
        password: admin123
        auth_source: admin

test:
  clients:
    default:
      database: dimensa_api_test
      hosts:
        - localhost:27017
      options:
        user: admin
        password: admin123
        auth_source: admin
```

### 4.3 Test the connection

```bash
bin/rails runner "puts Mongoid.default_client.database.name"
# Expected: dimensa_api_development
```

---

## STEP 5 - Configure RSpec

### 5.1 Install RSpec

```bash
bin/rails generate rspec:install
```

This creates:
- `.rspec` — RSpec options
- `spec/spec_helper.rb` — general config
- `spec/rails_helper.rb` — Rails-specific config

### 5.2 Add Mongoid cleanup to `spec/rails_helper.rb`

Add inside the `RSpec.configure` block:

```ruby
config.before(:each) do
  Mongoid.purge!
end
```

### 5.3 Disable host authorization for tests

Edit `config/environments/test.rb` and add inside the `configure` block:

```ruby
config.hosts.clear
```

### 5.4 Verify RSpec runs

```bash
bundle exec rspec
# 0 examples, 0 failures
```

---

## STEP 6 - Create the Cliente Model

### 6.1 Create `app/models/cliente.rb`

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

### 6.2 Create model spec `spec/models/cliente_spec.rb`

```ruby
require "rails_helper"

RSpec.describe Cliente, type: :model do
  let(:valid_attributes) do
    { nome: "Joao Silva", email: "joao@email.com", telefone: "11999999999", documento: "12345678900" }
  end

  describe "validations" do
    it "is valid with all required attributes" do
      expect(Cliente.new(valid_attributes)).to be_valid
    end

    it "is invalid without nome" do
      expect(Cliente.new(valid_attributes.merge(nome: nil))).not_to be_valid
    end

    it "is invalid without email" do
      expect(Cliente.new(valid_attributes.merge(email: nil))).not_to be_valid
    end

    it "is invalid without documento" do
      expect(Cliente.new(valid_attributes.merge(documento: nil))).not_to be_valid
    end

    it "is invalid with duplicate email" do
      Cliente.create!(valid_attributes)
      expect(Cliente.new(valid_attributes.merge(documento: "99999999999"))).not_to be_valid
    end

    it "is invalid with duplicate documento" do
      Cliente.create!(valid_attributes)
      expect(Cliente.new(valid_attributes.merge(email: "other@email.com"))).not_to be_valid
    end
  end
end
```

### 6.3 Run model tests

```bash
bundle exec rspec spec/models/
# 6 examples, 0 failures
```

---

## STEP 7 - Create the API Controller (JSON)

### 7.1 Generate the controller

```bash
bin/rails g controller api/v1/clientes --no-helper --no-assets --skip-template-engine
```

### 7.2 Edit `app/controllers/api/v1/clientes_controller.rb`

```ruby
module Api
  module V1
    class ClientesController < ApplicationController
      before_action :set_cliente, only: %i[show update destroy]

      def index
        render json: Cliente.all
      end

      def show
        render json: @cliente
      end

      def create
        @cliente = Cliente.new(cliente_params)
        if @cliente.save
          render json: @cliente, status: :created
        else
          render json: { errors: @cliente.errors.full_messages }, status: :unprocessable_entity
        end
      end

      def update
        if @cliente.update(cliente_params)
          render json: @cliente
        else
          render json: { errors: @cliente.errors.full_messages }, status: :unprocessable_entity
        end
      end

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

### 7.3 Add API routes in `config/routes.rb`

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

### 7.4 Create API spec `spec/requests/api/v1/clientes_spec.rb`

```ruby
require "rails_helper"

RSpec.describe "Api::V1::Clientes", type: :request do
  let(:valid_attributes) do
    { nome: "Joao Silva", email: "joao@email.com", telefone: "11999999999", documento: "12345678900" }
  end

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

### 7.5 Run all tests

```bash
bundle exec rspec
# 11 examples, 0 failures
```

---

## STEP 8 - Test the API Manually

Start the server:

```bash
bin/rails server
```

In another terminal, test with `curl`:

```bash
# Create
curl -s -X POST http://localhost:3000/api/v1/clientes \
  -H "Content-Type: application/json" \
  -d '{"cliente":{"nome":"Maria","email":"maria@email.com","telefone":"11888888888","documento":"98765432100"}}' | jq

# List
curl -s http://localhost:3000/api/v1/clientes | jq

# Show (replace ID)
curl -s http://localhost:3000/api/v1/clientes/<ID> | jq

# Update (replace ID)
curl -s -X PATCH http://localhost:3000/api/v1/clientes/<ID> \
  -H "Content-Type: application/json" \
  -d '{"cliente":{"nome":"Maria Santos"}}' | jq

# Delete (replace ID)
curl -s -X DELETE http://localhost:3000/api/v1/clientes/<ID> -w "%{http_code}\n"
```

---

## STEP 9 - Test the API Through the Browser

Since this is a JSON API, the browser can directly display responses for GET requests.
For POST, PATCH and DELETE you can use the **browser DevTools console** (F12 > Console).

### 9.1 Start the server

```bash
cd dimensa_api
bin/rails server
```

> The server starts at `http://localhost:3000`.

### 9.2 Allow the host (development)

Rails 8 blocks unknown hosts by default. If you see a "Blocked host" error,
edit `config/environments/development.rb` and add inside the `configure` block:

```ruby
config.hosts << "localhost"
```

### 9.3 GET requests (browser address bar)

Open your browser and navigate to:

| Action         | URL                                              |
|----------------|--------------------------------------------------|
| Health check   | `http://localhost:3000/up`                       |
| List clientes  | `http://localhost:3000/api/v1/clientes`          |
| Show cliente   | `http://localhost:3000/api/v1/clientes/<ID>`     |

> **Tip:** Install a browser extension like **JSON Formatter** (Chrome/Edge) to view
> JSON responses with syntax highlighting and collapsible sections.

### 9.4 POST, PATCH, DELETE (browser DevTools console)

Open the browser DevTools with **F12**, go to the **Console** tab, and paste the following JavaScript snippets.

#### Create a cliente (POST)

```javascript
fetch("http://localhost:3000/api/v1/clientes", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({
    cliente: {
      nome: "Maria Silva",
      email: "maria@email.com",
      telefone: "11888888888",
      documento: "98765432100"
    }
  })
})
.then(res => res.json())
.then(data => console.log(data));
```

Expected output:
```json
{
  "_id": { "$oid": "..." },
  "nome": "Maria Silva",
  "email": "maria@email.com",
  "telefone": "11888888888",
  "documento": "98765432100",
  "created_at": "...",
  "updated_at": "..."
}
```

> Copy the `$oid` value — you will need it for the next requests.

#### Update a cliente (PATCH)

```javascript
fetch("http://localhost:3000/api/v1/clientes/<ID>", {
  method: "PATCH",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({
    cliente: { nome: "Maria Santos" }
  })
})
.then(res => res.json())
.then(data => console.log(data));
```

#### Delete a cliente (DELETE)

```javascript
fetch("http://localhost:3000/api/v1/clientes/69c93d196c3adaf4c5e2c4dc", {
  method: "DELETE"
})
.then(res => console.log("Status:", res.status));
// Expected: Status: 204
```

#### Verify deletion (GET)

Refresh `http://localhost:3000/api/v1/clientes` in the address bar — the deleted record should be gone.

### 9.5 Full test flow

1. Open `http://localhost:3000/api/v1/clientes` — should return `[]`
2. In the console, run the **POST** snippet to create a cliente
3. Refresh the page — should return an array with 1 item
4. Copy the `$oid` and open `http://localhost:3000/api/v1/clientes/<ID>` — shows the single record
5. In the console, run the **PATCH** snippet with the ID — name should update
6. Refresh the page to confirm the update
7. In the console, run the **DELETE** snippet with the ID
8. Refresh the list page — should return `[]` again

---

## Summary

| What             | Where                                      |
|------------------|---------------------------------------------|
| MongoDB          | Docker container on `localhost:27017`       |
| Model            | `app/models/cliente.rb`                     |
| API Controller   | `app/controllers/api/v1/clientes_controller.rb` |
| Routes           | `config/routes.rb` → `/api/v1/clientes`    |
| Model tests      | `spec/models/cliente_spec.rb`               |
| API tests        | `spec/requests/api/v1/clientes_spec.rb`     |
