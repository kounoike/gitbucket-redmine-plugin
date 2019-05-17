package controller

import gitbucket.core.api.JsonFormat
import gitbucket.core.controller.ControllerBase
import gitbucket.core.service._
import gitbucket.core.util._
import gitbucket.core.util.Implicits._
import gitbucket.core.util.OwnerAuthenticator
import model.RedmineConfig
import org.scalatra.forms._
import service.RedmineService

object RedmineController {
  case class RedmineConfigForm(
    enableRedmine: Boolean,
    redmineUrl: String,
    redmineProject: String,
    apiKey: String
  )
}

class RedmineController
  extends ControllerBase
  with RepositoryService
  with AccountService
  with RedmineService
  with IssuesService
  with IssueCreationService
  with PrioritiesService
  with MilestonesService
  with WebHookIssueCommentService
  with PullRequestService
  with MergeService
  with CommitsService
  with ActivityService
  with WebHookPullRequestReviewCommentService
  with LabelsService
  with OwnerAuthenticator
{
  import RedmineController._
  val redmineConfigForm = mapping(
    "enableRedmine" -> trim(label("Enable Redmine", boolean())),
    "redmineUrl" -> trim(label("Redmine URL", text(required))),
    "redmineProject" -> trim(label("Redmine project name", text(required))),
    "apiKey" -> trim(label("Redmine API Key", text(required))),
  )(RedmineConfigForm.apply)

  get("/:owner/:repository/settings/redmine")(ownerOnly{ repository =>
    html.config(repository, loadRedmineConfig(repository.owner, repository.name), flash.get("info"))
  })

  post("/:owner/:repository/settings/redmine", redmineConfigForm)(ownerOnly { (form, repository) =>
    if(form.enableRedmine){
      val redmineUrl = if(form.redmineUrl.endsWith("/")){form.redmineUrl}else{form.redmineUrl + "/"}
      saveRedmineConfig(repository.owner, repository.name, Some(
        RedmineConfig(
          repository.owner,
          repository.name,
          form.redmineUrl,
          form.redmineProject,
          form.apiKey
        )
      ))
    }else{
      saveRedmineConfig(repository.owner, repository.name, None)
    }
    flash += "info" -> "Redmine configuration has been updated."
    redirect(s"/${repository.owner}/${repository.name}/settings/redmine")
  })

  post("/api/v3/redmine/:owner/:repository/webhook")(ownerOnly{ repository =>
    println("Redmine-plugin webhook")
    println(repository)
    println(request.body)

    syncIssues(repository)

    JsonFormat("Ok")
  })
}
