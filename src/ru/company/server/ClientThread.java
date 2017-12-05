package ru.company.server;
import java.io.*;
import java.net.Socket;
public class ClientThread {
    public ServerController myServer;
    public Socket socket;
    public ObjectOutputStream out;
    public ObjectInputStream in;
    public boolean bisy;
    public ClientThread(ServerController serverController, Socket socket, boolean bisy) {
        this.socket=socket;
        this.myServer=serverController;
        this.bisy=bisy;
        try {
            /*создание каналов для передачи матрицы*/
            this.out=new ObjectOutputStream(socket.getOutputStream());
            this.in =new ObjectInputStream(socket.getInputStream());
            new Thread(()-> {
                myServer.subscribe(this);    //добавление клиента
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public int  sendMsgMatrix(final int [][] matrix1, final int [][] matrix2, WorkElement elementWork,
                              final int [][]resultmatrix) {
        int element = 0;
        try {
            out.writeInt(matrix1.length); //writeInt - системная функция, которая записывает в буфер out-а размер матрицы
            out.flush();                  //выталкиваем из буфера out-а
            for (int i = 0; i < matrix1.length; i++) {
                for (int j = 0; j < matrix1.length; j++) {
                    out.writeInt(matrix1[i][j]);
                    out.flush();
                }
            }
            for (int i = 0; i < matrix2.length; i++) {
                for (int j = 0; j < matrix2.length; j++) {
                    out.writeInt(matrix2[i][j]);
                    out.flush();
                }
            }
            out.writeInt(elementWork.index_i);
            out.flush();
            out.writeInt(elementWork.index_j);
            out.flush();
            out.writeBoolean(elementWork.flag_doing);
            out.flush();
            out.writeBoolean(elementWork.busy = true);
            out.flush();
            this.bisy = false;
            element = in.readInt();                   //считываем текущий элемент посчитанной матрицы из канала
            System.out.println(element);              //вывод посчитанного элемента на экран
            resultmatrix[elementWork.index_i][elementWork.index_j] = element;
        } catch (IOException e) {
            ServerController serverController=new ServerController();
            serverController.unsubscribe(this);//вызов функции удаление клиента изсписка (текущий клиент)
            System.out.println("закрыли сокет!!!");
        }
        return element;
    }
}
