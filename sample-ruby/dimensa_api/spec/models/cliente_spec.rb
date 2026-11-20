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
