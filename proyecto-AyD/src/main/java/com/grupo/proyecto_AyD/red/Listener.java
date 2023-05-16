package com.grupo.proyecto_AyD.red;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupo.proyecto_AyD.controlador.ControladorChat;
import com.grupo.proyecto_AyD.controlador.ControladorConectar;
import com.grupo.proyecto_AyD.controlador.ControladorConectarServidor;
import com.grupo.proyecto_AyD.controlador.ControladorLlamada;
import com.grupo.proyecto_AyD.dtos.SolicitudLlamadaDTO;
import com.grupo.proyecto_AyD.modelo.Mensaje;
import com.grupo.proyecto_AyD.modelo.Sesion;
import com.grupo.proyecto_AyD.modelo.Usuario;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Clase que se encarga de escuchar las conexiones entrantes, con arquitectura P2P
 * @see com.grupo.proyecto_AyD.red.ListenerServidor
 * que sigue la arquitectura cliente-servidor
 */
public class Listener implements ChatInterface {
    private ServerSocket serverSocket;
    private Socket socket;
    private BufferedReader in;
    private boolean eschuchando = false;

    private ObjectMapper mapper;
    private Usuario usuario;
    private String puerto;
    private String ip;
    private final List<SolicitudLlamadaDTO> solicitudes = new ArrayList<>();

    private static Listener listener;


    public void init(String ip, int puerto, boolean desdeChat) {
        this.usuario = Usuario.getUsuario();

        try {
            usuario.setIp(InetAddress.getLocalHost().getHostAddress());
            serverSocket = new ServerSocket(puerto);

            this.eschuchando = true;
            mapper = new ObjectMapper();

            escuchar(desdeChat);

            System.out.println("Escuchando en puerto: " + usuario.getPuerto());
        } catch (IOException e) {
            System.out.println("Error al iniciar el listener: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Mensaje> enviarMensaje(String mensaje) {
        return new ArrayList<>();
    }


    private void escuchar(boolean desdeChat) {
        Thread thread = new Thread(() -> {
            try {
                ControladorChat controlador = null;

                while (eschuchando) {
                    System.out.println("Esperando conexion..." + serverSocket.toString());
                    Socket soc = serverSocket.accept();
                    in = new BufferedReader(new InputStreamReader(soc.getInputStream()));

                    String mensajeCrudo = in.readLine();
                    System.out.println("MENSAJE RECIBIDO: " + mensajeCrudo);

                    if (mensajeCrudo != null) {
                        Mensaje mensaje = mapper.readValue(mensajeCrudo, Mensaje.class);
                        String contenido = mensaje.getMensaje();

                        if (contenido.contains("[CONTROL]")) {
                            contenido = contenido.replace("[CONTROL]", "");
                            if (contenido.contains("[CONEXION_CLIENTE][OK]")) {
                                ControladorConectarServidor.confirmarConexion();
                            }

                            if (contenido.contains("PUERTO:")) {
                                this.puerto = mensaje.getMensaje().replace("[CONTROL]PUERTO:", "");
                            }

                            if (contenido.contains("IP:")) {
                                this.ip = mensaje.getMensaje().replace("[CONTROL]IP:", "");
                            }

                            if (contenido.contains("[FINALIZAR_CHAT]")) {
                                assert controlador != null;

                                pararEscucha();
                                controlador.finalizarChat();
                            }

                            if (contenido.contains("[CONECTAR][SOLICITUD]")) {
                                contenido = contenido.replace("[CONECTAR][SOLICITUD]", "");
                                SolicitudLlamadaDTO solicitud = mapper.readValue(contenido, SolicitudLlamadaDTO.class);
                                solicitudes.add(solicitud);

                                ControladorLlamada.getControladorLlamada().setDatosSolicitud(solicitud);
                            }

                            if (contenido.contains("[CONECTAR][ERROR]")) {
                                contenido = contenido.replace("[CONECTAR][ERROR]", "");
                                ControladorConectar.getControlador().setEstado(contenido);
                            }

                        } else {
                            // Controlador nunca deberia ser null, primero llegan los mensajes de control
                            Sesion sesion = Sesion.getSesion();
                            sesion.getMensajes().add(mensaje);
                            assert controlador != null;

                            controlador
                                    .getVistaChat()
                                    .setMensajes(
                                            sesion
                                                .getMensajes()
                                                .stream()
                                                .sorted(Comparator.comparing(Mensaje::getFecha))
                                                .toList()
                                    );
                        }
                    }
                }
            } catch (Exception e){
                System.out.println(e.getMessage());
            }
        });

        thread.start();
    }

    public void pararEscucha() {
        usuario.finalizarEscucha();
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.out.println("Error al cerrar el socket: " + e.getMessage());
        }
        this.eschuchando = false;
    }

    public static Listener getListener(){
        if (listener == null) {
            listener = new Listener();
        }

        return  listener;
    }
}