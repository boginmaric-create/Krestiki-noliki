using System;

namespace TicTacToe
{
    class Program
    {
        static char[] board = { '1', '2', '3', '4', '5', '6', '7', '8', '9' };

        static void Main(string[] args)
        {
            DrawBoard();
            Console.Write("Выберите номер клетки (1-9): ");
            string input = Console.ReadLine();

            if (!int.TryParse(input, out int choice) || choice < 1 || choice > 9)
            {
                Console.WriteLine("Ошибка: Введен некорректный символ!");
                return;
            }

            if (board[choice - 1] == 'X' || board[choice - 1] == 'O')
            {
                Console.WriteLine("Ошибка: Эта клетка уже занята!");
                return;
            }

            Console.WriteLine($"Ход принят в клетку {choice}");
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
