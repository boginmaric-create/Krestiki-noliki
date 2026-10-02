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
                    Console.WriteLine("Некорректный ввод! Нажмите Enter...");
                    Console.ReadLine();
                    continue;
                }

                board[choice - 1] = currentSymbol;
                currentPlayer = (currentPlayer == 1) ? 2 : 1;
            }
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
