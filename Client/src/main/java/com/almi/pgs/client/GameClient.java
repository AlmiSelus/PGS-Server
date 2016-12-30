package com.almi.pgs.client;

import com.almi.pgs.commons.Constants;
import com.almi.pgs.game.GamePacket;
import com.almi.pgs.germancoding.rudp.ReliableSocket;
import com.google.gson.Gson;
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * Created by Almi on 2016-12-10.
 */
public class GameClient extends SimpleApplication {

    private final static Logger log = LoggerFactory.getLogger(GameClient.class);

    private Geometry red;
    private Geometry blue;
    private Boolean isRunning = true;
    private ReliableSocket server;

    public GameClient(String[] gameStringParams) {

    }

    @Override
    public void simpleInitApp() {
        Box box1 = new Box(1,1,1);
        blue = new Geometry("Box", box1);
        blue.setLocalTranslation(new Vector3f(1,-1,1));
        Material mat1 = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat1.setColor("Color", ColorRGBA.Blue);
        blue.setMaterial(mat1);

        /** create a red box straight above the blue one at (1,3,1) */
        Box box2 = new Box(1,1,1);
        red = new Geometry("Box", box2);
        red.setLocalTranslation(new Vector3f(1,3,1));
        Material mat2 = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat2.setColor("Color", ColorRGBA.Red);
        red.setMaterial(mat2);

        rootNode.attachChild(blue);
        rootNode.attachChild(red);

        initKeys();
        try {
            server = new ReliableSocket();
            server.connect(new InetSocketAddress("127.0.0.1", Constants.PORT));

        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        // make the player rotate:
        blue.rotate(0, 2*tpf, 0);
    }

    private void initKeys() {
        // You can map one or several inputs to one named action
        inputManager.addMapping("Pause",  new KeyTrigger(KeyInput.KEY_P));
        inputManager.addMapping("Left",   new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("Right",  new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping("Rotate", new KeyTrigger(KeyInput.KEY_SPACE),
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        // Add the names to the action listener.
        inputManager.addListener(actionListener,"Pause");
        inputManager.addListener(analogListener, "Left", "Right", "Rotate");

    }

    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean keyPressed, float tpf) {
            if (name.equals("Pause") && !keyPressed) {
                isRunning = !isRunning;
            }
        }
    };

    private AnalogListener analogListener = new AnalogListener() {
        public void onAnalog(String name, float value, float tpf) {
            if (isRunning) {
                if (name.equals("Rotate")) {
                    red.rotate(0, value*speed, 0);
                }
                if (name.equals("Right")) {
                    Vector3f v = red.getLocalTranslation();
                    red.setLocalTranslation(v.x + value*speed, v.y, v.z);
                }
                if (name.equals("Left")) {
                    Vector3f v = red.getLocalTranslation();
                    red.setLocalTranslation(v.x - value*speed, v.y, v.z);
                }

                Vector3f redTranslation = red.getWorldTranslation();
                Quaternion redRotation    = red.getWorldRotation();
                GamePacket movementPacket = new GamePacket(redTranslation.getX(),
                        redTranslation.getY(),
                        redTranslation.getZ(),
                        redRotation.getW(),
                        redRotation.getX(),
                        redRotation.getY(),
                        redRotation.getZ(),
                        (byte)1,
                        (byte)1);
                Gson gson = new Gson();
                String jsonToSend = gson.toJson(movementPacket);
                log.info(jsonToSend);
                try {
                    send(jsonToSend);
                } catch (Exception e) {
                    log.info(ExceptionUtils.getStackTrace(e));
                }
            } else {
                System.out.println("Press P to unpause.");
            }
        }
    };

    private void send(String serializedData) throws Exception {
        OutputStream os = server.getOutputStream();
        os.write(serializedData.getBytes());
        os.flush();
    }
}
