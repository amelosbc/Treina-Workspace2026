import java.util.Scanner;

public class ExercicioExpressoesJava {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int opcao;

        do {
            System.out.println("\n===== Exercícios - Expressões Java =====");
            System.out.println("1. Nome e idade");
            System.out.println("2. Área do quadrado");
            System.out.println("3. Área do retângulo");
            System.out.println("4. Diferença de idade");
            System.out.println("0. Sair");
            System.out.print("Escolha um exercício: ");
            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {
                case 1:
                    exercicio1(scanner);
                    break;
                case 2:
                    exercicio2(scanner);
                    break;
                case 3:
                    exercicio3(scanner);
                    break;
                case 4:
                    exercicio4(scanner);
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida!");
            }
        } while (opcao != 0);

        scanner.close();
    }

    // Exercício 1: Nome e ano de nascimento
    static void exercicio1(Scanner scanner) {
        System.out.print("\nDigite seu nome: ");
        String nome = scanner.nextLine();

        System.out.print("Digite seu ano de nascimento: ");
        int anoNascimento = scanner.nextInt();
        scanner.nextLine();

        int idade = 2026 - anoNascimento;

        System.out.println("Olá " + nome + " você tem " + idade + " anos");
    }

    // Exercício 2: Área do quadrado
    static void exercicio2(Scanner scanner) {
        System.out.print("\nDigite o tamanho do lado do quadrado: ");
        double lado = scanner.nextDouble();
        scanner.nextLine();

        double area = lado * lado;

        System.out.println("A área do quadrado é: " + area);
    }

    // Exercício 3: Área do retângulo
    static void exercicio3(Scanner scanner) {
        System.out.print("\nDigite a base do retângulo: ");
        double base = scanner.nextDouble();

        System.out.print("Digite a altura do retângulo: ");
        double altura = scanner.nextDouble();
        scanner.nextLine();

        double area = base * altura;

        System.out.println("A área do retângulo é: " + area);
    }

    // Exercício 4: Diferença de idade
    static void exercicio4(Scanner scanner) {
        System.out.print("\nDigite o nome da primeira pessoa: ");
        String nome1 = scanner.nextLine();

        System.out.print("Digite a idade da primeira pessoa: ");
        int idade1 = scanner.nextInt();
        scanner.nextLine();

        System.out.print("Digite o nome da segunda pessoa: ");
        String nome2 = scanner.nextLine();

        System.out.print("Digite a idade da segunda pessoa: ");
        int idade2 = scanner.nextInt();
        scanner.nextLine();

        int diferenca = Math.abs(idade1 - idade2);

        System.out.println("A diferença de idade entre " + nome1 + " e " + nome2 + " é de " + diferenca + " anos");
    }
}
