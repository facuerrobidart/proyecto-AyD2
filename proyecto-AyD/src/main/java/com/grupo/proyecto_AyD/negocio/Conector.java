package com.grupo.proyecto_AyD.negocio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Conector extends GestionDeRed {


    public Conector() {
        super();
    }

    private void conectar(String ip, int puerto) {
        try {
            System.out.println("Intentando conectar a: " + ip + ":" + puerto);
            socket = new Socket(ip, puerto);

            bufferEntrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bufferSalida = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Conectado a: " + ip + ":" + puerto);
        } catch (Exception e) {
            String mensaje = "Error conectando a: " + ip + ":" + puerto;
            System.out.println(mensaje);
            System.out.println(e.getMessage());

            throw new RuntimeException(mensaje);
        }
    }

    public void ejecutarConexion(String ip, int puerto) {
        Thread thread = new Thread(() -> {
            try {
                conectar(ip, puerto);
                this.enviarMensaje("INICIAR_SESION");
                while (conectado) {
                    recibirMensajes();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                cerrarConexion();
            }
        });

        thread.start();
    }
}
