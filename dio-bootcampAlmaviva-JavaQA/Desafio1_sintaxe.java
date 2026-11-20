import java.util.Scanner;

public class Desafio1_sintaxe {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Lê a linha de entrada contendo o nome do projeto
        String nomeProjeto = scanner.nextLine();

        // Converte o nome do projeto para letras maiúsculas e calcula seu comprimento
        String nomeEmMaiusculas = nomeProjeto.toUpperCase();
        int comprimento = nomeProjeto.length();

        // Imprime o nome em maiúsculas seguido de um espaço e do número de caracteres
        System.out.println(nomeEmMaiusculas + " " + comprimento);

        scanner.close();
    }
}
