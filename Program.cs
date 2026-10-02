using System;

namespace TicTacToe
{
    class Program
    {
        static char[] board = { '1', '2', '3', '4', '5', '6', '7', '8', '9' };
        static int currentPlayer = 1;

        static void Main(string[] args)
        {
            while (true)
            {
                Console.Clear();
                DrawBoard();

                char currentSymbol = (currentPlayer == 1) ? 'X' : 'O';
                Console.WriteLine($"Ход игрока {currentPlayer} ({currentSymbol})");
                Console.Write("Выберите номер клетки (1-9): ");

                string input = Console.ReadLine();

                if (!int.TryParse(input, out int choice) || choice < 1 || choice > 9 || board[choice - 1] == 'X' || board[choice - 1] == 'O')
                {
                    Console.WriteLine("Ошибка ввода! Нажмите Enter...");
                    Console.ReadLine();
                    continue;
                }

                board[choice - 1] = currentSymbol;

                if (CheckWin())
                {
                    Console.Clear();
                    DrawBoard();
                    Console.WriteLine($"Игрок {currentPlayer} ({currentSymbol}) Победил!");
                    break;
                }

                currentPlayer = (currentPlayer == 1) ? 2 : 1;
            }
        }

        static bool CheckWin()
        {
            int[,] winPatterns = new int[,]
            {
                {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
                {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
                {0, 4, 8}, {2, 4, 6}
            };

            for (int i = 0; i < 8; i++)
            {
                if (board[winPatterns[i, 0]] == board[winPatterns[i, 1]] &&
                    board[winPatterns[i, 1]] == board[winPatterns[i, 2]])
                    return true;
            }
            return false;
        }

        static void DrawBoard()
        {
            Console.WriteLine($" {board[0]} | {board[1]} | {board[2]} ");
            Console.WriteLine("---|---|---");
            Console.WriteLine($" {board[3]} | {board[4]} | {board[5]} ");
            Console.WriteLine("---|---|---");
            Console.WriteLine($" {board[6]} | {board[7]} | {board[8]} ");
        }
    }
}
