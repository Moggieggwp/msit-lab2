package msit.lb2;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

public class WumpusWorldEnvironment extends Agent {

    private boolean aBoolean = true;
    private boolean bump = false;
    private boolean scream = false;

    @Override
    protected void setup() {

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Cave_with_gold");
        sd.setName("Cave");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Environment agent " + getAID().getName() + " haS been initialized!");
        addBehaviour(new RequestBehavior());
    }

    private boolean arrowUsed = false;
    private boolean killed = false;
    private int dir = 0;

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Environment agent terminated!");
    }

    private class RequestBehavior extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.REQUEST)
                    myAgent.addBehaviour(new PerceptReplyBehaviour(msg));
                if (msg.getPerformative() == ACLMessage.CFP)
                    myAgent.addBehaviour(new ChangeBehaviour(msg));
            }
            else
            {
                block();
            }
        }

    }

    private int x = 0;
    private int y = 0;

    private class PerceptReplyBehaviour extends OneShotBehaviour {

        ACLMessage msg;

        PerceptReplyBehaviour(ACLMessage m)
        {
            super();
            msg = m;
        }

        public void action() {
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(WumpusWorldActions()[x][y]
                    + (bump ? "u" : "")
                    + (scream ? "c" : "")
                    + (aBoolean ? "" : "k"));
            myAgent.send(reply);
            System.out.println("Env: " + reply.getContent()
                    + String.format("\t (%d,%d) -> %d", x, y, dir));
            if (!aBoolean)
                doDelete();
        }
    }

    private class ChangeBehaviour extends OneShotBehaviour {

        ACLMessage message;

        ChangeBehaviour(ACLMessage message)
        {
            super();
            this.message = message;
        }

        public void action() {
            if (!aBoolean)
                throw new IllegalStateException("Agent is dead already!");
            bump = false;
            scream = false;
            String content = message.getContent().toLowerCase();
            if (content.contains("forward")) {
                if ((dir == 0 && y == 3)
                        ||(dir == 2 && y == 0)
                        ||(dir == 1 && x == 3)
                        ||(dir == 3 && x == 0)) {
                    bump = true;
                    return;
                }
                switch (dir) {
                    case 0:
                        y++;
                        break;
                    case 1:
                        x++;
                        break;
                    case 2:
                        y--;
                        break;
                    case 3:
                        x--;
                        break;
                }
                if (WumpusWorldActions()[x][y].contains("p")
                        || WumpusWorldActions()[x][y].contains("w")) {
                    aBoolean = false;
                }

            }
            else if (content.contains("shoot")){
                if (arrowUsed)
                    throw new IllegalStateException("Agent don't have an arrow!");
                else {
                    arrowUsed = true;
                    if (dir == 1) {
                        if (2 > y && 0 == x)
                            killed = true;
                    } else if (dir == 2) {
                        if (2 == y && 0 < x)
                            killed = true;
                    } else if (dir == 3) {
                        if (2 < y && 0 == x)
                            killed = true;
                    } else if (dir == 0) {
                        if (2 == y && 0 > x)
                            killed = true;
                    }
                    if (killed) {
                        scream = true;
                    }
                }

            }
            else if (content.contains("climb")){
                doDelete();
            }
            else if (content.contains("grab")){
                if (x == 2 && y == 1) {
                    String changedGold = "bs";
                    WumpusWorldActions()[2][1] = changedGold;
                    boolean goldTaken = true;
                }
                else
                    throw new IllegalStateException("There is no gold!");
            }
            else if (content.contains("right")){
                dir = (dir - 1) % 4;
            }
            else if (content.contains("left")){
                dir = (dir + 1) % 4;
            }
        }
    }
    private String[][] WumpusWorldActions() {
        String[][] a = {
                {"", "b", "p", "b"},
                {"s", "", "b", ""},
                {"w", "gbs", "p", "b"},
                {"s", "", "b", "p"}
        };
        return a;
    };
}
