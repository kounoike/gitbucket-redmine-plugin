package service

import com.taskadapter.redmineapi.RedmineManagerFactory
import gitbucket.core.controller.Context
import gitbucket.core.service._
import gitbucket.core.service.RepositoryService.RepositoryInfo
import gitbucket.core.model.Profile.profile.blockingApi._
import gitbucket.core.util.Implicits._
import model.RedmineConfig
import model.Profile._

import collection.JavaConverters._


trait RedmineService {
  self: IssueCreationService with IssuesService with AccountService with PullRequestService with MergeService =>

  def saveRedmineConfig(userName: String, repositoryName: String, config: Option[RedmineConfig])(implicit s: Session): Unit = {
    RedmineConfigs.filter { t =>
      (t.userName === userName.bind) && (t.repositoryName === repositoryName.bind)
    }.delete
    config.foreach{ config => RedmineConfigs += config}
  }

  def loadRedmineConfig(userName: String, repositoryName: String)(implicit s: Session): Option[RedmineConfig] = {
    RedmineConfigs.filter { t =>
      (t.userName === userName.bind) && (t.repositoryName === repositoryName.bind)
    }.firstOption
  }

  def syncIssues(repository: RepositoryInfo)(implicit context: Context, s: Session): Unit = {
    /*
    // RedmineのURL
    val url = "http://localhost/redmine/"
    // APIキー
    val accessKey = "9d12dec447200e1d09bdc98cf3043bc6145441b9"
    // プロジェクト名
    val project = "demo"
    */

    loadRedmineConfig(repository.owner, repository.name) match{
      case Some(config) =>
        val redmineUrl = config.redmineUrl
        val project = config.redmineProject
        val accessKey = config.apiKey

        val mgr = RedmineManagerFactory.createWithApiKey(redmineUrl, accessKey)

        val statuses = mgr.getIssueManager.getStatuses
        val nextStatusId = statuses.asScala.find(_.getName=="進行中").map(_.getId).getOrElse(0.asInstanceOf[Integer])
        val mergedStatusId = statuses.asScala.find(_.getName=="マージ済").map(_.getId).getOrElse(0.asInstanceOf[Integer])

        val issues = mgr.getIssueManager().getIssues(project, null)
        println(issues)

        issues.asScala.sortBy{i => i.getId}.foreach { issue =>
          val issueId = issue.getId
          println(issueId)
          println(issue.getTracker.getName)

          if(getIssue(repository.owner, repository.name, issueId.toString).isEmpty) {
            //val author = mgr.getUserManager.getUserById(issue.getAuthorId).getLogin
            val author = "root"
            getAccountByUserName(author).map{ account =>
              IssueId filter(_.byPrimaryKey(repository.owner, repository.name)) update(repository.owner, repository.name, issueId - 1)
              //createIssue(repository, issue.getSubject, Some("Dummy issue for Redmine"), None, None, None, Seq.empty, account)
              insertIssue(repository.owner, repository.name, author, issue.getSubject, Some("Dummy issue for redmine"), None, None, None)
            }.getOrElse{
              println("Not Found: ")
            }
          }

          if(issue.getTracker.getName == "PR"){
            //println(issue.getCustomFieldByName("base branch"))
            //println(issue.getCustomFieldByName("compare branch"))

            getIssue(repository.owner, repository.name, issueId.toString).map{ gbIssue =>
              if(!gbIssue.isPullRequest && issue.getStatusName == "新規"){
                val originBranch = issue.getCustomFieldByName("base branch").getValue
                val requestBranch = issue.getCustomFieldByName("compare branch").getValue
                getPullRequestCommitFromTo(repository, repository, originBranch, requestBranch) match {
                  case (Some(commitIdFrom), Some(commitIdTo)) =>
                    changeIssueToPullRequest(repository.owner, repository.name, issueId)
                    createPullRequest(
                      originRepository = repository,
                      issueId = issueId,
                      originBranch = originBranch,
                      requestUserName = repository.owner,
                      requestRepositoryName = repository.name,
                      requestBranch = requestBranch,
                      commitIdFrom = commitIdFrom.getName,
                      commitIdTo = commitIdTo.getName,
                      loginAccount = context.loginAccount.get
                    )

                    val url = s"${redmineUrl}/issues/${issueId}.json?key=${accessKey}"
                    val payload = s"""{"issue": {"status_id": ${nextStatusId}}"""
                    requests.put(url, headers = Map("X-Skip-Webhooks" -> "true"), data = payload)
                  case _ =>
                    None
                }
              }
              if(gbIssue.isPullRequest && issue.getStatusName == "マージ"){
                println("Will be merge")
                getPullRequest(repository.owner, repository.name, issueId) match {
                  case Some((gbi, pr)) =>
                    println("found")
                    mergePullRequest(repository, issueId, context.loginAccount.get, "", "merge-commit")

                    val url = s"${redmineUrl}/issues/${issueId}.json?key=${accessKey}"
                    val payload = s"""{"issue": {"status_id": ${mergedStatusId}}"""
                    requests.put(url, headers = Map("X-Skip-Webhooks" -> "true"), data = payload)
                  case _ =>
                    println("Not found")
                    None
                }
              }
            }
          }
        }
      case None => ()
    }
  }
}
