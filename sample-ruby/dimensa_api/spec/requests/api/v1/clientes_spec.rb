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
