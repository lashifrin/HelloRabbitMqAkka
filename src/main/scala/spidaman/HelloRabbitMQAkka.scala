package spidaman

import com.rabbitmq.client.{Channel, Connection, ConnectionFactory, QueueingConsumer}
import akka.actor._
import akka.util.duration._

/**
 * credit: @josdirksen's post at
 * http://www.smartjava.org/content/connect-rabbitmq-amqp-using-scala-play-and-akka
 * ...I just took out play 'cause I wanted "bare" akka
 */

object Config {
  	val RABBITMQ_HOST = "staging-api3"
  	val RABBITMQ_QUEUE = "dingus"
  	val RABBITMQ_EXCHANGE = "doofus"
}

object RabbitMQConnection {
 
  private val connection: Connection = null;
 
  /**
   * Return a connection if one doesn't exist. Else create
   * a new one
   */
  def getConnection(): Connection = {
    connection match {
      case null => {
        val factory = new ConnectionFactory();
        factory.setHost(Config.RABBITMQ_HOST);
        factory.newConnection();
      }
      case _ => connection
    }
  }
}

object Sender {

	implicit val system = ActorSystem("RabbitMQSystem")

  	def startSending = {
	    // create the connection
    	val connection = RabbitMQConnection.getConnection();
	    // create the channel we use to send
	    val sendingChannel = connection.createChannel();
	    // make sure the queue exists we want to send to
	    sendingChannel.queueDeclare(Config.RABBITMQ_QUEUE, false, false, false, null);
	 
	   	system.scheduler.schedule(2 seconds, 1 seconds, system.actorOf(Props(
            new SendingActor(channel = sendingChannel, queue = Config.RABBITMQ_QUEUE))), "MSG to Queue"
	   	)
 
	    val callback1 = (x: String) => println("Recieved on queue callback 1: %s".format(x));
	    setupListener(connection.createChannel(),Config.RABBITMQ_QUEUE, callback1);
	 
	    // create an actor that starts listening on the specified queue and passes the
	    // received message to the provided callback
	    val callback2 = (x: String) => println("Recieved on queue callback 2: %s".format(x));
	 
	    // setup the listener that sends to a specific queue using the SendingActor
	    setupListener(connection.createChannel(),Config.RABBITMQ_QUEUE, callback2);
  	}
 
  	private def setupListener(receivingChannel: Channel, queue: String, f: (String) => Any) {
    	system.scheduler.scheduleOnce(2 seconds, 
        	system.actorOf(Props(new ListeningActor(receivingChannel, queue, f))), "");
  	}

}
 
 class SendingActor(channel: Channel, queue: String) extends Actor {
 
  	def receive = {
    	case some: String => {
      		val msg = (some + " : " + System.currentTimeMillis());
      		channel.basicPublish("", queue, null, msg.getBytes());
      		println(msg);
    	}
    	case _ => {}
  	}
}

class ListeningActor(channel: Channel, queue: String, f: (String) => Any) extends Actor {
 
  	// called on the initial run
  	def receive = {
    	case _ => startReceving
  	}
 
  	def startReceving = {
 
	    val consumer = new QueueingConsumer(channel);
	    channel.basicConsume(queue, true, consumer);
	 
	    while (true) {
	      	// wait for the message
	      	val delivery = consumer.nextDelivery();
	      	val msg = new String(delivery.getBody());
	 
	      	// send the message to the provided callback function
	      	// and execute this in a subactor
	      	context.actorOf(Props(
	      		new Actor {
	        		def receive = {
	          			case some: String => f(some);
	        		}
	      		}
	      	)) ! msg
	    }
  	}
}

object Main {
	def main(args: Array[String]): Unit = {
		Sender.startSending
	}
}



