package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee._
import play.api.libs.ws.WS
import edu.nyu.cs.javagit.api._
import java.io._
import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.sys.process._
import scalax.io._
import scalax.io.JavaConverters._

object Application extends Controller {
  import Utility._
  
  //service state and streams
  @volatile private var serviceStarted = false
  @volatile private var serviceStream : Stream[String] = Stream.Empty
  @volatile private var serviceInput : Option[Output] = None

  def index = Action {
    Ok(views.html.index(JavaGitConfiguration.getGitVersion(),
                        commits, currentBranch, branches, currentCommit,
                        hostname))
  }

  def restart = Action {
    val pb = Process("make", makeDirFile)
    val pio = new ProcessIO(stdin => serviceInput = Some(stdin.asOutput),
                            stdout => serviceStream = scala.io.Source.fromInputStream(stdout).getLines.toStream,
                            _ => ())
    val p = pb.run(pio)
    serviceStarted = true
    Ok
  }
  
  def serviceLog = WebSocket.using[String] { request =>
    val in = Iteratee.foreach[String](x => serviceInput.get.write(x))
    val out = Enumerator.enumerate(serviceStream).andThen(Enumerator.eof)
    (in, out)
  }
  
  def stop = Action {
    //send `Ctrl-c a enter` to process stdin to terminate it 
    serviceInput.get.write("%c%c%c" format(3.toChar, 'a', 13.toChar))
    Ok
  } 
  
  def checkoutAll = Action {    
    Ok(gitCheckoutAll)
  }
  
  def checkoutBranchWithName(name : String) = Action {
    checkoutBranch(name)
    Ok  
  }
  
  def checkoutCommitWithHash(hash : String) = Action {    
    Ok(executeGitCommand("checkout %s" format(hash)))
  }
  
  def fetchAll = Action {
    Ok(executeGitCommand("fetch -a"))
  }
  
  def pullCurrent = Action {
    gitCheckoutAll
    Ok(executeGitCommand("pull --rebase"))
  }
  
  def restartCassandra = Action {
    "sudo service cassandra restart".!
    Ok
  }
  
  def stopCassandra = Action {
    "sudo service cassandra stop".!
    Ok
  }
  
  protected def statusOk = {
    serviceStarted = true
    Ok("Ok")
  } 
  
  protected def statusFail = {
    serviceStarted = false
    Ok("Fail")
  } 
  
  def status = Action {
    val pingFuture =
      WS.url("http://localhost:8888/apps/ping")
      .get().map(x => Some(x.body))
      .recover { case e:java.net.ConnectException => Some("Refused")}    
    val timeoutFuture = Promise.timeout(Some("Fail"), 2.seconds)    
    Async {
      scala.concurrent.Future.firstCompletedOf(Seq(pingFuture, timeoutFuture)).map {
        case Some("pong") => statusOk
        case x => statusFail
      }
    }
  }
}
