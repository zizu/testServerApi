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
import edu.nyu.cs.javagit.api.commands.GitCheckout

object Application extends Controller {
  private lazy val rootDir = """/home/%s/testServerApi""" format(System.getProperty("user.name"))
  private lazy val wtDir = """%s/Expert""" format(rootDir)
  private lazy val makeDirFile = new java.io.File("""%s/Expert/src/code/""" format(rootDir))

  private def wt     = WorkingTree.getInstance(wtDir)
  private def dotGit = wt.getDotGit()
  
  //service state and streams
  private var serviceStarted = false
  private var serviceStream : Stream[String] = Stream.Empty
  private var serviceInput : Option[Output] = None
  
  private def executeGitCommand(cmd : String) = 
    "git --git-dir=%s/.git %s".format(wtDir, cmd).!!
  
  private def fetchAll = {
    executeGitCommand("fetch -a")
  }

  private def commits =
    dotGit.getLog().toList.take(20).map(x => (x.getSha(), x.getDateString(), x.getAuthor(), x.getMessage()))

  private def branches =
    //dotGit.getBranches().toList.map(_.getName())
    executeGitCommand("branch -a").split("\n").map(x => x.substring(1).trim().split("""/""").toList.last).toSet.toList

  private def currentBranch =
    try { wt.getCurrentBranch().getName() }
    catch { case _:Exception => ""}

  private def checkoutBranch(branchName : String) {
    val command = """checkout -f %s""" format(branchName)
    val fallbackCommand = """checkout -b %s origin/%s""" format(branchName, branchName)
    executeGitCommand("checkout .")
    try { executeGitCommand(command) } 
    catch { case _:Exception => executeGitCommand(fallbackCommand) }
  }
  
  private def currentCommit = 
    executeGitCommand("""log --pretty=oneline""").split("\n").map(_.split(" ").toList).map({case x::xs => (x, xs.mkString(" ")) }).head
    
  def index = Action {
    fetchAll
    Ok(views.html.index(JavaGitConfiguration.getGitVersion(),
                        commits, currentBranch, branches, currentCommit))
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
    val in = Iteratee.consume[String]()
    val out = Enumerator.enumerate(serviceStream).andThen(Enumerator.eof)
    (in, out)
  }
  
  def stop = Action {
    //send `Ctrl-c a enter` to process stdin to terminate it 
    serviceInput.get.write("%c%c%c" format(3.toChar, 'a', 13.toChar))
    Ok
  } 
  
  def checkoutAll = Action {    
    Ok(executeGitCommand("checkout ."))
  }
  
  def checkoutBranchWithName(name : String) = Action {
    checkoutBranch(name)
    Ok  
  }
  
  def checkoutCommitWithHash(hash : String) = Action {    
    Ok(executeGitCommand("checkout %s" format(hash)))
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
