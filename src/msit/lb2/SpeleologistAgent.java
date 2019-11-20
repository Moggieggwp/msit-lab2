package msit.lb2;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class SpeleologistAgent extends Agent {

    private AID na;
    private AID ea;

    @Override
    protected void setup() {
        addBehaviour(new WakerBehaviour(this,500) {
            @Override
            protected void onWake() {
                DFAgentDescription navigatorDescription = new DFAgentDescription();
                DFAgentDescription environmentDescription = new DFAgentDescription();
                ServiceDescription navigatorService = new ServiceDescription();
                ServiceDescription environmentService = new ServiceDescription();
                navigatorService.setType("Navigator");
                environmentService.setType("Cave_with_gold");
                navigatorDescription.addServices(navigatorService);
                environmentDescription.addServices(environmentService);
                try {
                    na = DFService.search(myAgent, navigatorDescription)[0].getName();
                    ea = DFService.search(myAgent, environmentDescription)[0].getName();
                }
                catch (FIPAException fe) {
                    fe.printStackTrace();
                }
                System.out.println("Speleologist agent " + getAID().getLocalName() + " has been initialized!");
                myAgent.addBehaviour(new CaveBehaviour());
            }
        });

    }

    private class CaveBehaviour extends Behaviour {

        private int step = 0;
        private MessageTemplate mt;
        private String message;

        private String GeneratePerceptText(String content) {
            StringBuilder temp = new StringBuilder();
            if (content.contains("k")) {
                System.out.println("Agent is finalized.");
                temp.append("Killed..");
            } else {
                if (content.contains("s"))
                    temp.append(String.format(dict, "stench"));
                if (content.contains("b"))
                    temp.append(String.format(dict, "breeze"));
                if (content.contains("g"))
                    temp.append(String.format(dict, "glitter"));
                if (content.contains("u"))
                    temp.append(String.format(dict, "bump"));
                if (content.contains("c"))
                    temp.append(String.format(dict, "scream"));

                temp.append("What should I do?");
            }
            return temp.toString();
        }

        private String ProcessSentence(String content) {
            if (content.contains("forward"))
                return "Forward";
            else if (content.contains("shoot"))
                return "Shoot";
            else if (content.contains("climb"))
                return "Climb";
            else if (content.contains("grab"))
                return "Grab";
            else if (content.contains("right"))
                return "TurnRight";
            else if (content.contains("left"))
                return "TurnLeft";
            throw new IllegalStateException("Unexpected action!");
        }

        private boolean alive = true;
        private String dict = "%s is here. ";

        @Override
        public void action() {
            switch (step) {
                case 0:
                    ACLMessage requestPercept = new ACLMessage(ACLMessage.REQUEST);
                    requestPercept.addReceiver(ea);
                    requestPercept.setConversationId("Get-percepts");
                    myAgent.send(requestPercept);
                    mt = MessageTemplate.MatchConversationId("Get-percepts");

                    step++;
                    break;
                case 1:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            message = GeneratePerceptText(reply.getContent());
                            ACLMessage askForAction = new ACLMessage(ACLMessage.REQUEST);
                            askForAction.addReceiver(na);
                            askForAction.setContent(message);
                            askForAction.setConversationId("action");
                            System.out.println(getAID().getLocalName() + ": " + message);
                            myAgent.send(askForAction);
                            mt = MessageTemplate.MatchConversationId("action");
                            step++;
                        }
                    } else {
                        block();
                    }
                    break;
                case 2:
                    ACLMessage aclMessage = myAgent.receive(mt);
                    if (aclMessage != null) {
                        if (aclMessage.getPerformative() == ACLMessage.PROPOSE) {
                            message = ProcessSentence(aclMessage.getContent());
                            step++;
                        }
                    } else {
                        block();
                    }
                    break;
                case 3:
                    ACLMessage aclMessage1 = new ACLMessage(ACLMessage.CFP);
                    aclMessage1.addReceiver(ea);
                    aclMessage1.setContent(message);
                    aclMessage1.setConversationId("action");
                    System.out.println(getAID().getLocalName() + " (to env): "+ message);
                    myAgent.send(aclMessage1);
                    mt = MessageTemplate.and(
                            MessageTemplate.MatchConversationId("action"),
                            MessageTemplate.MatchInReplyTo(aclMessage1.getReplyWith()));
                    step++;
                    break;
                case 4:
                    if (message == "Climb") {
                        step++;
                        doDelete();
                        return;
                    }
                    else
                        step=0;
                    break;

            }
        }

        @Override
        public boolean done() {
            return step == 5 || !alive;
        }
    }
}
