# ADR-0001: Autenticação com Clerk e Perfil de Desenvolvimento Local

* **Status:** Aceito
* **Data:** 2026-11-12

## Contexto
O TaskManager precisa de autenticação de usuários para suportar multi-inquilinato pessoal (cada usuário tem seus próprios quadros). Escolhemos o Clerk com OTP (One-Time Password) pela facilidade no frontend e modernidade de segurança. No entanto, depender do Clerk para todo e qualquer teste de desenvolvimento local, testes automatizados e exploração via Swagger tornaria a produtividade lenta.

## Decisão
1. **Ambiente de Produção / Padrão:** Spring Security configurado como OAuth2 Resource Server validando JWT emitido pelo Clerk via JWKS (JSON Web Key Set).
2. **Ambiente de Desenvolvimento (`dev` profile):** Criar um filtro de autenticação de desenvolvimento que:
   - Se houver header `Authorization: Bearer <jwt>`, valida com o Clerk normalmente.
   - Se houver header `X-Mock-User: <user_id>`, autentica automaticamente como esse usuário mock sem contatar serviços externos.
   - Rotas de documentação (`/swagger-ui/**`, `/api-docs/**`) e console (`/h2-console/**`) permanecem públicas.

## Consequências
* **Positivas:** Permite codificar e testar 100% das regras de negócio e endpoints no Swagger ou via `curl` sem necessidade de criar conta no Clerk antecipadamente.
* **Negativas:** Requer atenção para nunca habilitar o filtro de mock em ambiente de produção (controlado via `@Profile("dev")`).
