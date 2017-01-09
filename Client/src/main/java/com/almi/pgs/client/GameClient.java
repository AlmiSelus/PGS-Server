package com.almi.pgs.client;

import com.almi.pgs.commons.Constants;
import com.almi.pgs.game.PacketListener;
import com.almi.pgs.game.PacketManager;
import com.almi.pgs.game.packets.*;
import com.almi.pgs.germancoding.rudp.ReliableSocket;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.*;

import static com.almi.pgs.commons.Constants.*;

/**
 * Created by Almi on 2016-12-10.
 */
public class GameClient extends SimpleApplication implements ScreenController {

    private final static Logger log = LoggerFactory.getLogger(GameClient.class);
    private Boolean isRunning = false;
    private ReliableSocket server;
    private Player player;
    private SendMessageThread senderThread = new SendMessageThread();

    /**
     * GUI Related stuff
     */
    private NiftyJmeDisplay niftyDisplay;
    private Nifty nifty;
    private Element popup;

    /**
     * Packet manager manages all communication with server through ReliableSocket. Use this manager
     * for sending data and add packet listeners to receive certain packet
     */
    private PacketManager packetManager = new PacketManager();
    /**
     * PlayerID - Player (if need arises - change value object type to whatever needed).
     * Current player does not have entry in this map!!! use other variables!
     */
    private final Map<Byte, Player> players = Collections.synchronizedMap(new HashMap<>());

    public GameClient(String[] gameStringParams) {
        packetManager.addPacketListener(new ClientAuthPacketListener());
        packetManager.addPacketListener(new ClientGamePacketListener(this));
        packetManager.addPacketListener(new ClientLogoutPacketListener(this));
        packetManager.addPacketListener(new ClientGameTickListener(this));
		packetManager.addPacketListener(new PlayerTeleportListener());
    }

    @Override
    public void simpleInitApp() {
        setPauseOnLostFocus(false);
		assetManager.registerLocator(new File("Client/assets").getAbsolutePath(), FileLocator.class);
		flyCam.setMoveSpeed(20);
		this.setLevel();
		this.setLight();

        initKeys();
        try {
            server = new ReliableSocket();
            server.setReceiveBufferSize(RECEIVE_BUFFER_SIZE);
            server.setSendBufferSize(SEND_BUFFER_SIZE);
            server.connect(new InetSocketAddress("127.0.0.1", Constants.PORT));

        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
        }
        new ReceiverThread().start();
        senderThread.start();

        niftyDisplay = new NiftyJmeDisplay(assetManager,
                inputManager,
                audioRenderer,
                guiViewPort);
        nifty = niftyDisplay.getNifty();

        nifty.fromXml("Interface/Login_Popup.xml", "start", this);

        guiViewPort.addProcessor(niftyDisplay);
        flyCam.setDragToRotate(true);

        popup = nifty.createPopup("popup");
    }

    @Override
    public void simpleUpdate(float tpf) {

    }

    private void initKeys() {
        // You can map one or several inputs to one named action
        inputManager.addMapping("Login",  new KeyTrigger(KeyInput.KEY_G));
        inputManager.addMapping("Pause",  new KeyTrigger(KeyInput.KEY_P));
        inputManager.addMapping("Left",   new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right",  new KeyTrigger(KeyInput.KEY_D));
		inputManager.addMapping("Forward",  new KeyTrigger(KeyInput.KEY_W));
		inputManager.addMapping("Backward",  new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Space", new KeyTrigger(KeyInput.KEY_SPACE));
        // Add the names to the action listener.
        inputManager.addListener(actionListener,"Pause");
        inputManager.addListener(analogListener, "Login", "Left", "Right", "Left", "Forward", "Backward", "Space");

        inputManager.addMapping("RotateX", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("RotateY", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping("RotateX_negative", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("RotateY_negative", new MouseAxisTrigger(MouseInput.AXIS_Y, false));


        inputManager.addListener((AnalogListener) (name, value, tpf) -> {

            if("RotateX".equals(name) || "RotateX_negative".equals(name) || "RotateY".equals(name) || "RotateY_negative".equals(name)) {
                senderThread.action();
            }
        }, "RotateX", "RotateX_negative", "RotateY", "RotateY_negative");
    }

    private ActionListener actionListener = (name, keyPressed, tpf) -> {
        if (name.equals("Pause") && !keyPressed) {
            isRunning = !isRunning;
        }
    };

    private AnalogListener analogListener = new AnalogListener() {
        public void onAnalog(String name, float value, float tpf) {
            if (isRunning) {
                if (name.equals("Space")) {
					shoot();
                }
                senderThread.action();
            }
        }

		private void shoot() {
            CollisionResults results = new CollisionResults();
            Ray ray = new Ray(cam.getLocation(), cam.getDirection());
            rootNode.collideWith(ray, results);
            System.out.println("----- Collisions? " + results.size() + "-----");
            for (int i = 0; i < results.size(); i++) {
                float dist = results.getCollision(i).getDistance();
                Vector3f pt = results.getCollision(i).getContactPoint();
                String hit = results.getCollision(i).getGeometry().getName();
                System.out.println("* Collision #" + i);
                System.out.println("  You shot " + hit + " at " + pt + ", " + dist + " wu away.");
            }
            if (results.size() > 0) {
                CollisionResult closest = results.getClosestCollision();
                byte victimId = (byte) Integer.parseInt(closest.getGeometry().getName());
                packetManager.sendPacket(server, new ShootPacket(player.getPlayerId(), victimId));

            }
		}
    };

    @Override
    public void bind(Nifty nifty, Screen screen) {

    }

    @Override
    public void onStartScreen() {

    }

    @Override
    public void onEndScreen() {

    }

    public void tryLogIn() {
        TextField loginBox      = nifty.getScreen("start").findNiftyControl("login", TextField.class);
        TextField passwordBox   = nifty.getScreen("start").findNiftyControl("password", TextField.class);
        String login = loginBox.getText();
        String password = passwordBox.getText();
        log.info("Login = " + login);
        log.info("Password = " + password);
        AuthPacket authPacket = new AuthPacket(login, password);
        packetManager.sendPacket(server, authPacket);
        log.info("Packet sent");

    }

    private class ReceiverThread extends Thread {

        public ReceiverThread() {
        }

        @Override
        public void run() {
            try {
                InputStream is = server.getInputStream();
                log.info("In here");
                while(true) {
                    receive(is);
                }
            } catch (Exception e) {
                //TODO server disconnected ... notify user on this.
                log.info(ExceptionUtils.getStackTrace(e));
            }
        }

        private void receive(InputStream is) throws Exception {
            while(true) {
                String packetString = "";
                boolean end = false;

                while(!end) {
                    byte[] buffer = new byte[1024];
                    int c = is.read(buffer);
                    packetString += new String(buffer, 0, c);
                    if(packetString.contains("}**")) {
                        end = true;
                    }
                }

                Packet packet = packetManager.getGeneralGamePacket(packetString);
                if(!SILENT_MODE) {
                    log.info(Objects.isNull(packet) + "");
                }

                if (packet != null) {
                    packetManager.handlePacket(packet);
                }
            }
        }
    }

    private class SendMessageThread extends Thread {
        public SendMessageThread() {
        }

        @Override
        public void run() {
            while(true){}
        }


        void action() {
            if(isRunning) {
                Vector3f redTranslation = cam.getLocation();

				redTranslation.y = -5f;
				cam.setLocation(redTranslation);

                Quaternion redRotation = cam.getRotation();
                GamePacket movementPacket = new GamePacket(redTranslation.getX(),
                        redTranslation.getY(),
                        redTranslation.getZ(),
                        redRotation.getW(),
                        redRotation.getX(),
                        redRotation.getY(),
                        redRotation.getZ());
                if(player != null) {
                    movementPacket.setPlayerID(player.getPlayerId());
                    movementPacket.setTeam(player.getTeam());
                }
                movementPacket.setCurrentTime(new Date().getTime());
                if(!SILENT_MODE) {
                    log.info("Sending = " + movementPacket);
                }
                packetManager.sendPacket(server, movementPacket);
                if(player != null) {
                    switch (player.getTeam()) {
                        case 0:
                            if (redTranslation.z > 100) {
                                log.info("WINNER");
                                sendFlagPacket();
                            }

                        case 1:
                            if (redTranslation.z < -100) {
                                log.info("WINNER");
                                sendFlagPacket();
                            }
                    }
                }
            }
        }
    }

	private void sendFlagPacket() {
		PlayerTakeFlagPacket packet = new PlayerTakeFlagPacket();
		packet.setPlayerId(player.getPlayerId());
		packetManager.sendPacket(server, packet);
		isRunning = false;
	}

    @Override
    public void destroy() {
        if(player != null) {
            packetManager.sendPacket(server, new LogoutPacket(new Date().getTime(), player.getPlayerId()));
        }
        super.destroy();
    }

    private class ClientGameTickListener implements PacketListener {
        private GameClient client;

        private boolean isWinnerDisplayed = true;

        public ClientGameTickListener(GameClient gc) {
            client = gc;
        }

        @Override
        public void handlePacket(Packet gamePacket) {
            GameState gameState = (GameState) gamePacket;
            if (player != null) {
                isRunning = gameState.getIsRunning() == 1;
                flyCam.setEnabled(isRunning);
                if(gameState.getIsRunning() == 1) {
                    int mins = gameState.getRemainingTime() / 6000;
                    int sec = (gameState.getRemainingTime() % 6000);

                    gameState.getPositionMap().entrySet().stream().filter(playerPosition -> players.containsKey(playerPosition.getKey())).forEach(playerPosition -> {
                        client.enqueue(() -> {
                            client.getRootNode().getChild("Player" + playerPosition.getKey()).setLocalTranslation(playerPosition.getValue());
                            return null;
                        });
                    });

                    gameState.getRotationMap().entrySet().stream().filter(playerRotation ->
                            players.containsKey(playerRotation.getKey())).forEach(playerRotation -> client.enqueue(() -> {
                                client.getRootNode().getChild("Player" + playerRotation.getKey()).setLocalRotation(playerRotation.getValue());
                                return null;
                            }));

                    /**
                     * Reset winner message
                     */
                    Screen screen = nifty.getCurrentScreen();
                    if(screen.getScreenId().equals("hud")) {

                        if (isWinnerDisplayed) {
                            screen.findElementByName("winner").getRenderer(TextRenderer.class).setText("");
                            isWinnerDisplayed = false;
                        }
                        screen.findElementByName("remainigTime").getRenderer(TextRenderer.class).setText(mins + ":" + sec);
                        screen.findElementByName("aTeamPoints").getRenderer(TextRenderer.class).setText(String.valueOf(gameState.getPointsBlue()));
                        screen.findElementByName("bTeamPoints").getRenderer(TextRenderer.class).setText(String.valueOf(gameState.getPointsRed()));
                    }
                } else {
                    Screen screen = nifty.getCurrentScreen();
                    if(screen.getScreenId().equals("hud")) {
                        isWinnerDisplayed = true;
                        String winner = "Winner : ";
                        winner += gameState.getWinner() == 0 ? "Team Blue" : gameState.getWinner() == 2 ? "Tie!" : "Team Red";
                        screen.findElementByName("winner").getRenderer(TextRenderer.class).setText(winner);
                    }
                }
            }
        }

        @Override
        public Class<? extends Packet> packetClass() {
            return GameState.class;
        }
    }

	private class PlayerTeleportListener implements PacketListener {

        private boolean isWinnerDisplayed = true;

        @Override
        public void handlePacket(Packet gamePacket) {
            PlayerTeleportPacket pt = (PlayerTeleportPacket) gamePacket;
			cam.setLocation(new Vector3f(0f, -5f, player.getTeam() == 0 ? -110 : 110));
			cam.lookAt(new Vector3f(0f, -5f, 0f), new Vector3f(0f, 1f, 0f));
        }

        @Override
        public Class<? extends Packet> packetClass() {
			return PlayerTeleportPacket.class;
        }
    }

    private class ClientLogoutPacketListener implements PacketListener {

        private final GameClient client;

        public ClientLogoutPacketListener(GameClient client) {
            this.client = client;
        }

        @Override
        public void handlePacket(Packet gamePacket) {
            LogoutPacket logoutPacket = (LogoutPacket)gamePacket;
            synchronized (players) {
                if (players.containsKey(logoutPacket.getPlayerID())) {
                    Spatial spatial = players.get(logoutPacket.getPlayerID()).getGeometry();
                    players.remove(logoutPacket.getPlayerID());
                    client.enqueue(() -> client.getRootNode().detachChild(spatial));
                }
            }
        }

        @Override
        public Class<? extends Packet> packetClass() {
            return LogoutPacket.class;
        }
    }

    private class ClientAuthPacketListener implements PacketListener {

        @Override
        public void handlePacket(Packet gamePacket) {
            if(gamePacket != null) {
                AuthResponsePacket authResponse = (AuthResponsePacket) gamePacket;
                if(!SILENT_MODE) {
                    log.info("Response code = " + authResponse.getCode());
                }
                if(authResponse.getCode() == 200) {
                    if(!SILENT_MODE) {
                        log.info("client ok");
                    }
					player = new Player(authResponse.getPlayerID(), authResponse.getTeamID(), authResponse.getHash());
                    isRunning = true;
                    nifty.gotoScreen("hud");
                } else {
                    popup.findElementByName("issue").getRenderer(TextRenderer.class).setText(authResponse.getReason());
                    nifty.showPopup(nifty.getCurrentScreen(), popup.getId(), null);

                    log.info(authResponse.getReason());
                }
            }
        }

        @Override
        public Class<? extends Packet> packetClass() {
            return AuthResponsePacket.class;
        }
    }

    private class ClientGamePacketListener implements PacketListener {

        private final GameClient client;

        private ClientGamePacketListener(GameClient client) {
            this.client = client;
        }

        @Override
        public void handlePacket(Packet packet) {
            if(packet != null) {
                GamePacket gamePacket = (GamePacket) packet;
//                log.info("Packet = " + gamePacket);
                if (player.getPlayerId() != 1 && player.getPlayerId() != 0) {
                    log.info("PID = " + player.getPlayerId());
                }
                if (player.getPlayerId() != gamePacket.getPlayerID()) {
                    Player player = players.get(gamePacket.getPlayerID());
                    if (player == null) {
                        player = new Player(gamePacket);
                        Geometry g = getNewPlayerGeometry(player.getPlayerId(), player.getTeam());
                        player.setGeometry(g);
                        client.enqueue(() -> rootNode.attachChild(g));
                        players.put(player.getPlayerId(), player);
                    }
                    if (gamePacket.getCurrentTime() >= player.getPacketTime()) {
                        players.get(gamePacket.getPlayerID()).setPacketTime(gamePacket.getCurrentTime());
                        Player finalPlayer = player;
                        client.enqueue(() -> {
                            Geometry g = (Geometry) rootNode.getChild("Player" + finalPlayer.getPlayerId());
                            g.setLocalTranslation(gamePacket.getX(), gamePacket.getY(), gamePacket.getZ());
                            g.setLocalRotation(
                                    new Quaternion(gamePacket.getxAngle(),
                                            gamePacket.getyAngle(),
                                            gamePacket.getzAngle(),
                                            gamePacket.getW()));
                            //                            players.get(gamePacket.getPlayerID()).setNewGamePacket(gamePacket);
                            return null;
                        });
                    }
                }
            }
		}

        @Override
        public Class<? extends Packet> packetClass() {
            return GamePacket.class;
        }
    }

	private void setLevel() {
		Spatial arena = assetManager.loadModel("arena.obj");
		arena.setLocalScale(0.6f);
		arena.setLocalTranslation(0, -20, 0);
		arena.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
		rootNode.attachChild(arena);
	}

	private void setLight() {
		DirectionalLight sun = new DirectionalLight();
		sun.setDirection(new Vector3f(1f, -1f, 1f).normalizeLocal());
		rootNode.addLight(sun);

		final int SHADOWMAP_SIZE=1024;
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 3);
        dlsr.setLight(sun);
        viewPort.addProcessor(dlsr);

        DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, SHADOWMAP_SIZE, 3);
        dlsf.setLight(sun);
        dlsf.setEnabled(true);
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.addFilter(dlsf);
        viewPort.addProcessor(fpp);

		AmbientLight al = new AmbientLight();
		al.setColor(ColorRGBA.White.mult(1.3f));
		rootNode.addLight(al);
	}

	private Geometry getNewPlayerGeometry(byte playerID, byte team) {
		Box box1 = new Box(1, 1, 1);
		Geometry geometry = new Geometry("Player"+playerID, box1);
		geometry.setLocalTranslation(new Vector3f(1, -1, 1));
		Material mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		mat1.setColor("Color", player.getTeam() == 0 ?
                new ColorRGBA((float)(34/255.0), (float)(167/255.0),(float)(240/255.0), 1.0F) :
                new ColorRGBA((float)(239/255.0), (float)(72/255.0), (float)(54/255.0), 1.0F));
		geometry.setMaterial(mat1);
		return geometry;
	}
}
