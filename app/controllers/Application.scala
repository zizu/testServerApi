package controllers

import play.api._
import play.api.mvc._
import edu.nyu.cs.javagit.api._
import java.io._
import scala.collection.JavaConversions._

object Application extends Controller {
  private lazy val rootDir = """/home/%s/testServerApi""" format(System.getProperty("user.name"))
  private lazy val wtDir = """$s/Expert""" format(rootDir)

  private def wt     = WorkingTree.getInstance(wtDir)
  private def dotGit = wt.getDotGit()

  private def checkRepository = {

  }

  private def commits =
    dotGit.getLog().toList.take(20).map(x => (x.getSha(), x.getDateString(), x.getAuthor(), x.getMessage()))

  private def branches =
    dotGit.getBranches().toList.map(_.getName())

  private def checkoutBranch(name : String) {
    wt.checkout(dotGit.getBranches().toList.find(x => x.getName() == name).get)
  }

  // private def checkoutCommit(hash : String) {
  //   wt.checkout(commits.find(x => x._2 == hash).get._1)
  // }

  def index = Action {
    Ok(views.html.index(JavaGitConfiguration.getGitVersion()))
  }

}
