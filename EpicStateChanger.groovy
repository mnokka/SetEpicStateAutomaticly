/*
 
When new Story has added to Epic, change Epic state back to In Progress

Epic forward movements are following automaticly linked Stories movements. 
This listener will implement "In case Epic has new Story added, it state should be changed automaticly to back to In Progress state".
 Workflow do have buttons for this but automation is needed
 
 To be implemented as a JIRA listener (for Issue Created event and Parent Issue edited )
 
 
 January 2018 mika.nokka1@gmail.com
 */
 
 
 
 import com.atlassian.jira.component.ComponentAccessor
 import com.atlassian.jira.issue.Issue
 import com.atlassian.jira.issue.MutableIssue
 import com.atlassian.jira.issue.customfields.option.LazyLoadedOption
 import org.apache.log4j.Logger
 import org.apache.log4j.Level
 import com.atlassian.jira.issue.link.IssueLinkTypeManager
 import com.atlassian.jira.issue.CustomFieldManager
 import com.atlassian.jira.issue.fields.CustomField
 
 
 import com.atlassian.jira.issue.history.ChangeItemBean
 import com.atlassian.jira.event.project.VersionReleaseEvent
 import com.atlassian.jira.issue.IssueFieldConstants // for DUE_DATE
 import com.atlassian.jira.issue.fields.CustomField
 import java.sql.Timestamp
 import com.atlassian.jira.issue.ModifiedValue
 import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
 import com.atlassian.jira.issue.Issue
 
 import com.atlassian.jira.event.type.EventType
 import com.atlassian.jira.user.ApplicationUser
 import com.atlassian.jira.event.type.EventDispatchOption
 import java.util.ArrayList
 import java.util.Collection
 import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
 
 import com.atlassian.jira.component.ComponentAccessor
 import com.atlassian.jira.issue.MutableIssue
 import com.atlassian.jira.issue.CustomFieldManager
 import com.atlassian.jira.issue.status.Status
 import com.atlassian.jira.bc.issue.IssueService
 import com.atlassian.jira.issue.IssueInputParameters;
 import com.atlassian.jira.issue.IssueInputParametersImpl;
 
 
 
 // CONFIGURATIONS:
 def AdminUser = "mikanokka" // this user must be rights to drive statuses
 def ReviewToInProgress=71   // Review ---> In Progress
 // END OF CONFIGURATIONS
 
 def changeManager = ComponentAccessor.getChangeHistoryManager();
 def issueManager = ComponentAccessor.getIssueManager()
 def CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
 def CustomField epicLinkField = customFieldManager.getCustomFieldObjectByName('Epic Link');
 def IssueService issueService = ComponentAccessor.getComponent(IssueService);
 def IssueInputParameters issueInputParameters = new IssueInputParametersImpl([:]);
 def util = ComponentAccessor.getUserUtil()
 
 // set logging to Jira log
 def log = Logger.getLogger("EpicStateChanger") // change for customer system
 log.setLevel(Level.DEBUG) // DEBUG INFO
  
 
 log.debug("---------- EpicStateChanger started -----------")
 //log.debug("Event:"+event)
 //log.debug("Event: ${event.getEventTypeId()} fired for ${event.issue} and caught by listener")
 //log.debug("ISSUE_CREATED_ID VALUE: ${EventType.ISSUE_CREATED_ID}")
 
 def issue = event.issue as Issue //from Adaptavista examples
 def type=issue.getIssueType().getName() // Jira7
 
 log.debug("Issue created event,type:"+type)
 
 if (type=="Story" || type=="Task")  {
	 log.debug("TypeTaskHit:"+type)
	 
	 MutableIssue EpicIssue = issue.getCustomFieldValue(customFieldManager.getCustomFieldObjectByName("Epic Link")) as MutableIssue
	 if (EpicIssue) {
		 log.info("Epic exists for created issue (${issue}),it is:"+EpicIssue)
		 //def String epicIssue = EpicIssue.getValue(issue)
		 def status=EpicIssue.getStatus().getName()
		 log.debug("Current Issue's Epic Status:"+status)
		 //EpicIssue.setStatus("In Progress")
		 if (status=="In Progress") {
			 log.debug("Epic In Progress, do nothing")
		 }
		 
		 // Review ---> In Progress
		 if (status=="Review") {
			 log.info("Epic in Review, moving back to In Progress")
			 EpicId=EpicIssue.id
			 log.debug("EpicID :"+EpicId)
			 assignee=EpicIssue.getAssignee()
			 
			 TargetStateID=ReviewToInProgress // ReviewToInProgress
			 if (assignee) {
				 log.debug("Epic has assignee OK:"+assignee)
				 transitUser=assignee
				 DriveEpicState(TargetStateID,transitUser,EpicId,issueService,issueInputParameters)
			 }
			 else {
				 log.info("Epic Has NO assignee, usign default user")
				 ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(util.getUserByName("${AdminUser}"))
				 whoisthis2=ComponentAccessor.getJiraAuthenticationContext().getUser()
				 log.debug("Changed script user: {$whoisthis2}")
				 transitUser=whoisthis2
				 DriveEpicState(TargetStateID,transitUser,EpicId,issueService,issueInputParameters)
			 }
		 }	 
	 }
	 else {
		 log.info("No Epic found for this issues. Exiting")
		 return
	 }
 	}
 

// Move issue to new state using transit ID  
def DriveEpicState(TargetStateID,transitUser,EpicId,issueService,issueInputParameters) {
	 // TargetStateID: Hardcoded transit value from workflow current state -----TargetStateID---> New state
	 // transitUser: User to do the transit
	 // EpicId: Epic ID to be driven newstate
	 IssueService.TransitionValidationResult validationResult =
		 issueService.validateTransition(transitUser,
			 EpicId, TargetStateID as Integer, issueInputParameters)
 
		 def errorCollection = validationResult.errorCollection
		 log.error(errorCollection)
		 if (! errorCollection.hasAnyErrors()) {
			 issueService.transition(transitUser, validationResult)
			 log.error("Transit:${TargetStateID} done OK")
		 }
		 else {
			 log.error("ERROR: Transit:${TargetStateID} gone wrong!!")
		 } 
	}

 
 
 log.debug("---------- EpicStateChanger stopped -----------")