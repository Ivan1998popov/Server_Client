package ru.company.server;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Vector;

import static com.sun.xml.internal.messaging.saaj.packaging.mime.util.ASCIIUtility.getBytes;

public class ServerController extends Thread implements Initializable {
    public int [][] firstMatrix ;
    public int [] []secondMatrix;
    public int [] []resultMatrix;
    public ServerSocket serverSocket;  //для установки соединения
    public DatagramSocket ds;
    public List<ClientThread> clients= new Vector<>();
    public List<WorkElement> elementsWork=new Vector<>();
    public Socket s = null;             //канал, по которому будут передаваться данные
    @FXML
    public  TextArea textArea;
    @FXML
    TextArea ip_server;
    @FXML
    Button Super;
    @FXML
    TextField msgField;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            serverSocket = new ServerSocket(7790);
            InetAddress ipAddress = InetAddress.getLocalHost();                  //получение ip адреса сервера
            ip_server.appendText("Адресс сервера: "+String.valueOf(ipAddress));  //печать текста в окно в виде строки
            ds = new DatagramSocket(2001,InetAddress.getByName("0.0.0.0")); //канал тот, которые рассылает
            UDP_broadcast();                                                     //вызов метода
            connect();                                                           //вызов интода
        } catch (IOException e) {
            e.printStackTrace();                                                 //печатать ошибки
        }
    }
    public void setSizeMatrix() {
        /*Integer.parseInt(msgField.getText())- перево размера матрицы со строки в числа*/
          firstMatrix=new int[Integer.parseInt(msgField.getText())][Integer.parseInt(msgField.getText())];
          secondMatrix=new int[Integer.parseInt(msgField.getText())][Integer.parseInt(msgField.getText())];
          random(firstMatrix);  //вызов функции
          random(secondMatrix);
        for (int i = 0; i <firstMatrix.length ; i++) {
            for (int j = 0; j <firstMatrix.length ; j++) {
              elementsWork.add(new WorkElement(i,j,false,false));
            }
        }
    }
    public void UDP_broadcast(){
        new Thread(()-> {
            while (true) {
                try {
                    byte[] b = getBytes("h");  //h - переводим в байды
                    /*DatagramPacket - формирует пакет для получения мусора - системный класс*/
                    DatagramPacket receive = new DatagramPacket(b, b.length);
                    ds.receive(receive);          //получение (receive) мусора и его размера (d_send) по каналу (ds)
                    /*receive.getAddress() - считывает откуда был получен мусор*/
                    DatagramPacket send = new DatagramPacket(b, b.length, receive.getAddress(), receive.getPort());
                    ds.send(send);                //отправка - ответ
                    System.out.println("Рассылка пакета");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public void connect(){
        new Thread(()-> {
            try {
                while (true) {
                    System.out.println("Сервер ждет подключения клиента...");
                    textArea.appendText("Сервер ждет подключения клиента... \n");
                    s = serverSocket.accept(); //прослушка,сервером порта
                    System.out.println("Клиент подключен к серверу...");
                    textArea.appendText("Клиент " + s.getPort() + " подключен к серверу...\n");
                    new ClientThread(this, s, false);
                    //ClientThread - передаем матрицы клиентам и получаем сервером
                    //сервер ловит подключение
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    public void random(final int[][] matrix){
        final Random random = new Random();                     // Генератор случайных чисел.
        for (int row = 0; row < matrix.length; ++row)           // Цикл по строкам матрицы.
            for (int col = 0; col < matrix[row].length; ++col)  // Цикл по столбцам матрицы.
                matrix[row][col] = random.nextInt(10)+1; //элементы от 1 до10
        /*выделение памяти под результирующую матрицу*/
        resultMatrix=new int[firstMatrix.length][firstMatrix.length];
    }
    public void connectClient () {
        new Thread(()->{
        while (true) {
            try {
                for (ClientThread o : clients) {
                    if (o.bisy != true) {
                        for (WorkElement workElement : elementsWork) {
                            /*если элемент не посчитан и не занят*/
                            if (workElement.flag_doing != true && workElement.busy != true) {
                                /*то клиент занят*/
                                o.bisy = true;
                                try {
                                    resultMatrix[workElement.index_i][workElement.index_j] =
                                            o.sendMsgMatrix(firstMatrix, secondMatrix, workElement, resultMatrix);
                                    //ClientThread - передаем матрицы клиентам и получаемсервером
                                }catch (Exception e){
                                    unsubscribe(o);     //
                                    workElement.flag_doing=false;
                                    workElement.busy=false;
                                    break;
                                }
                                o.bisy = false;
                                break;
                            }
                        }
                    }

                }
            } catch (Exception e) {
                System.out.println("Клиент был удален!");
            }
            if (resultMatrix[resultMatrix.length - 1][resultMatrix.length - 1] != 0) {

                printAllMatrix("C:\\Users\\Иван\\Desktop\\matrix", firstMatrix, secondMatrix, resultMatrix);
                System.out.println("Матрица посчитана!");
                textArea.appendText("Матрица посчитана! \n");
                break;
            }


        }

        }).start();

    }

    private static void printAllMatrix(final String fileName,
                                       final int[][] firstMatrix,
                                       final int[][] secondMatrix,
                                       final int[][] resultMatrix)
    {
        try (final FileWriter fileWriter = new FileWriter(fileName, false)) {
            fileWriter.write("First matrix:\n");
            printMatrix(fileWriter, firstMatrix);

            fileWriter.write("\nSecond matrix:\n");
            printMatrix(fileWriter, secondMatrix);

            fileWriter.write("\nResult matrix:\n");
            printMatrix(fileWriter, resultMatrix);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printMatrix(final FileWriter fileWriter,
                                    final int[][] matrix) throws IOException
    {
        boolean hasNegative = false;  // Признак наличия в матрице отрицательных чисел.
        int     maxValue    = 0;      // Максимальное по модулю число в матрице.

        // Вычисляем максимальное по модулю число в матрице и проверяем на наличие отрицательных чисел.
        for (final int[] row : matrix) {  // Цикл по строкам матрицы.
            for (final int element : row) {  // Цикл по столбцам матрицы.
                int temp = element;
                if (element < 0) {
                    hasNegative = true;
                    temp = -temp;
                }
                if (temp > maxValue)
                    maxValue = temp;
            }
        }

        // Вычисление длины позиции под число.
        int len = Integer.toString(maxValue).length() + 1;  // Одно знакоместо под разделитель (пробел).
        if (hasNegative)
            ++len;  // Если есть отрицательные, добавляем знакоместо под минус.

        // Построение строки формата.
        final String formatString = "%" + len + "d";

        // Вывод элементов матрицы в файл.
        for (final int[] row : matrix) {  // Цикл по строкам матрицы.
            for (final int element : row)  // Цикл по столбцам матрицы.
                fileWriter.write(String.format(formatString, element));

            fileWriter.write("\n");  // Разделяем строки матрицы переводом строки.
        }
    }

    public synchronized void unsubscribe(ClientThread o) {
       textArea.appendText("Клиент с портом "+ o.socket.getPort()+ " отключен\n");
        System.out.println("Клиент с портом "+ o.socket.getPort()+ " отключен\n");
        clients.remove(o);
    }
    public synchronized void subscribe(ClientThread o) {
        clients.add(o);
        System.out.println(clients.lastIndexOf(o));
    }



}
