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
