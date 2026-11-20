import java.util.Scanner;

public class ExerciciosEstruturasDeControle {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int opcao;

        do {
            System.out.println("\n===== Exercícios - Estruturas de Controle =====");
            System.out.println("1. Tabuada");
            System.out.println("2. Cálculo do IMC");
            System.out.println("3. Pares ou ímpares em intervalo");
            System.out.println("4. Divisibilidade contínua");
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

    // Exercício 1: Tabuada de 1 até 10
    static void exercicio1(Scanner scanner) {
        System.out.print("\nDigite um número para ver sua tabuada: ");
        int numero = scanner.nextInt();
        scanner.nextLine();

        System.out.println("\nTabuada do " + numero + ":");
        for (int i = 1; i <= 10; i++) {
            System.out.println(numero + " x " + i + " = " + (numero * i));
        }
    }

    // Exercício 2: Cálculo do IMC
    static void exercicio2(Scanner scanner) {
        System.out.print("\nDigite sua altura (em metros): ");
        double altura = scanner.nextDouble();

        System.out.print("Digite seu peso (em kg): ");
        double peso = scanner.nextDouble();
        scanner.nextLine();

        double imc = peso / (altura * altura);

        System.out.printf("\nSeu IMC é: %.2f\n", imc);

        if (imc <= 18.5) {
            System.out.println("Classificação: Abaixo do peso");
        } else if (imc <= 24.9) {
            System.out.println("Classificação: Peso ideal");
        } else if (imc <= 29.9) {
            System.out.println("Classificação: Levemente acima do peso");
        } else if (imc <= 34.9) {
            System.out.println("Classificação: Obesidade Grau I");
        } else if (imc <= 39.9) {
            System.out.println("Classificação: Obesidade Grau II (Severa)");
        } else {
            System.out.println("Classificação: Obesidade III (Mórbida)");
        }
    }

    // Exercício 3: Pares ou ímpares em intervalo decrescente
    static void exercicio3(Scanner scanner) {
        System.out.print("\nDigite o primeiro número: ");
        int primeiro = scanner.nextInt();

        System.out.print("Digite o segundo número (maior que o primeiro): ");
        int segundo = scanner.nextInt();
        scanner.nextLine();

        if (segundo <= primeiro) {
            System.out.println("Erro: o segundo número deve ser maior que o primeiro!");
            return;
        }

        System.out.print("Deseja ver os números pares ou ímpares? (par/impar): ");
        String opcao = scanner.nextLine().toLowerCase();

        if (!opcao.equals("par") && !opcao.equals("impar")) {
            System.out.println("Opção inválida! Digite 'par' ou 'impar'.");
            return;
        }

        int resto = opcao.equals("par") ? 0 : 1;

        System.out.println("\nNúmeros " + opcao + "es de " + segundo + " até " + primeiro + ":");
        for (int i = segundo; i >= primeiro; i--) {
            if (Math.abs(i % 2) == resto) {
                System.out.println(i);
            }
        }
    }

    // Exercício 4: Divisibilidade contínua
    static void exercicio4(Scanner scanner) {
        System.out.print("\nDigite o número inicial (divisor): ");
        int divisor = scanner.nextInt();
        scanner.nextLine();

        System.out.println("Digite outros números. O programa para quando o resto da divisão por " + divisor + " for diferente de 0.");
        System.out.println("Números menores que " + divisor + " serão ignorados.\n");

        while (true) {
            System.out.print("Digite um número: ");
            int numero = scanner.nextInt();
            scanner.nextLine();

            if (numero < divisor) {
                System.out.println("Número ignorado (menor que " + divisor + ").\n");
                continue;
            }

            if (numero % divisor != 0) {
                System.out.println(numero + " % " + divisor + " = " + (numero % divisor) + " (resto diferente de 0)");
                System.out.println("Programa encerrado!");
                break;
            }

            System.out.println(numero + " % " + divisor + " = 0 (divisível). Continue...\n");
        }
    }
}
