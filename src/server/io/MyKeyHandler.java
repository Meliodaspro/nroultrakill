package server.io;

/*
 *
 *
 * @author EMTI
 */

import network.example.KeyHandler;
import data.DataGame;
import network.session.ISession;

public class MyKeyHandler extends KeyHandler {

    @Override
    public void sendKey(ISession session) {
        super.sendKey(session);
        DataGame.sendDataImageVersion((MySession) session);
        DataGame.sendVersionRes((MySession) session);
    }

}
