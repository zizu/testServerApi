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

import scala.sys.process.Process
import scala.sys.process.ProcessIO
import scalax.io._
import scalax.io.JavaConverters._

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

  private def checkRepository = {

  }

  private def commits =
    dotGit.getLog().toList.take(20).map(x => (x.getSha(), x.getDateString(), x.getAuthor(), x.getMessage()))

  private def branches =
    dotGit.getBranches().toList.map(_.getName())

  private def currentBranch =
    wt.getCurrentBranch().getName()

  private def checkoutBranch(name : String) {
    wt.checkout(dotGit.getBranches().toList.find(x => x.getName() == name).get)
  }

  // private def checkoutCommit(hash : String) {
  //   wt.checkout(commits.find(x => x._2 == hash).get._1)
  // }

  def index = Action {
    Ok(views.html.index(JavaGitConfiguration.getGitVersion(),
                        commits, currentBranch, branches))
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
  
  //Comet socket
  def serviceLog = Action {
    Ok.stream(Enumerator.enumerate(serviceStream)).as(TEXT)
  }
  
  def stop = Action {
    //send `Ctrl-c a enter` to process stdin to terminate it 
    serviceInput.get.write("%c%c%c" format(3.toChar, 'a', 13.toChar))
    Ok
  } 
  
  def checkoutAll = TODO
  def checkoutBranchWithName(name : String) = TODO
  
  protected def statusOk = {
    serviceStarted = true
    Ok("Ok")
  } 
  
  protected def statusFail = {
    serviceStarted = false
    Ok("Fail")
  } 
  
  def status = Action {
    try {
      val pingFuture = WS.url("http://localhost:8888/apps/ping").get().map(x => x.body)    
      val timeoutFuture = Promise.timeout("Fail", 2.seconds)    
      Async {
        scala.concurrent.Future.firstCompletedOf(Seq(pingFuture, timeoutFuture)).map {
          case "pong" => statusOk
          case x => statusFail
        }
      }
    } catch {
      case _:Throwable => statusFail
    }
  }
}
