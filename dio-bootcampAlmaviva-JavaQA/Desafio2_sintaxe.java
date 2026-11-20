import java.util.Scanner;

public class Desafio2_sintaxe {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Lê a linha de entrada contendo o nome do projeto
        String nomeProjeto = scanner.nextLine();

        // Verifica se o nome é vazio ou contém apenas espaços
        if (nomeProjeto.trim().isEmpty()) {
            System.out.println("INVALIDO");
        } else {
            System.out.println(nomeProjeto.toUpperCase());
        }

        scanner.close();
    }
}
