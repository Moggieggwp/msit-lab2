package msit.lb2;

import aima.core.agent.Action;
import aima.core.agent.impl.DynamicAction;
import aima.core.environment.wumpusworld.AgentPercept;
import aima.core.environment.wumpusworld.HybridWumpusAgent;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class NavigatorAgent extends Agent {

    private HybridWumpusAgent logic;
    private boolean spelAlive = true;

    @Override
    protected void setup() {
        try {
            System.out.println("Navigator agent " + getAID().getLocalName() + " has been initialized!");
            DFAgentDescription dfAgentDescription = new DFAgentDescription();
            dfAgentDescription.setName(getAID());
            ServiceDescription serviceDescription = new ServiceDescription();
            serviceDescription.setType("Navigator");
            serviceDescription.setName("AIMA-Navigator");
            dfAgentDescription.addServices(serviceDescription);
            DFService.register(this, dfAgentDescription);

            logic = new HybridWumpusAgent();
            addBehaviour(new NavigatorBehaviour());
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Navigator agent has been terminated!");
    }

    private class NavigatorBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage message = myAgent.receive(MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("action")));
            if (message != null) {
                    myAgent.addBehaviour(new SendBehaviour(message));
            }
            else
            {
                block();
            }
        }

    }

    private class SendBehaviour extends OneShotBehaviour {

        private ACLMessage message;
        private String replyPattern = "You need to %s.";

        public  SendBehaviour(ACLMessage message)
        {
            super();
            this.message = message;
        }

        @Override
        public void action() {
            String content = message.getContent();
            ACLMessage reply = message.createReply();
            if (content != null) {
                reply.setPerformative(ACLMessage.PROPOSE);
                Action act = logic.execute(GetPercept(content));
                if (!spelAlive) {
                    doDelete();
                    System.out.println("What have I done?..");
                    return;
                }
                reply.setContent(CreateReplyContent(act));
            } else {
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent("not-available");
            }
            System.out.println("Navigator (to Speleologist): " + reply.getContent());
            myAgent.send(reply);
        }

        private AgentPercept GetPercept(String text) {
            text = text.toLowerCase();
            if (text.contains("killed")) {
                spelAlive = false;
            }
            AgentPercept agentPercept = new AgentPercept(text.contains("stench"),
                    text.contains("breeze"),
                    text.contains("glitter"),
                    text.contains("bump"),
                    text.contains("scream"));
            return agentPercept;
        }

        private String CreateReplyContent(Action act) {
            String actionCode = ((DynamicAction)act).getName();
            if (actionCode.contains("Turn"))
                actionCode = "turn " + actionCode.toLowerCase().substring(4);
            else if (actionCode.equals("Forward"))
                actionCode = "go forward";
            else
                actionCode = actionCode.toLowerCase();
            return String.format(replyPattern, actionCode);
        }
    }
}
