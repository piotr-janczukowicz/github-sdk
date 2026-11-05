package io.github.piotr_janczukowicz.github.sdk;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import io.github.piotr_janczukowicz.github.api.actions.GitHubCreateRepoVariablePayload;
import io.github.piotr_janczukowicz.github.api.actions.GitHubListWorkflowRunsForRepoFilter;
import io.github.piotr_janczukowicz.github.api.actions.GitHubOidcCustomSubRepo;
import io.github.piotr_janczukowicz.github.api.actions.GitHubSetCustomOidcSubClaimForRepoPayload;
import io.github.piotr_janczukowicz.github.api.activity.GitHubEvent;
import io.github.piotr_janczukowicz.github.api.activity.GitHubListRepoNotificationsForAuthenticatedUserFilter;
import io.github.piotr_janczukowicz.github.api.activity.GitHubSetRepoSubscriptionPayload;
import io.github.piotr_janczukowicz.github.api.agents.GitHubActionsSecret;
import io.github.piotr_janczukowicz.github.api.branches.GitHubListBranchesFilter;
import io.github.piotr_janczukowicz.github.api.branches.GitHubShortBranch;
import io.github.piotr_janczukowicz.github.api.codescanning.GitHubCodeScanningAlertItems;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubAddCollaboratorPayload;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubListCollaboratorsFilter;
import io.github.piotr_janczukowicz.github.api.collaborators.GitHubRepositoryCollaboratorPermission;
import io.github.piotr_janczukowicz.github.api.commits.GitHubListCommitsFilter;
import io.github.piotr_janczukowicz.github.api.dependabot.GitHubListAlertsForRepoFilter;
import io.github.piotr_janczukowicz.github.api.deploykeys.GitHubCreateDeployKeyPayload;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubCreateDeploymentPayload;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubCreateDeploymentResult;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubCreateOrUpdateEnvironmentPayload;
import io.github.piotr_janczukowicz.github.api.deployments.GitHubListDeploymentsFilter;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubBlob;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubCreateRefPayload;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGetTreeFilter;
import io.github.piotr_janczukowicz.github.api.gitdatabase.GitHubGitTree;
import io.github.piotr_janczukowicz.github.api.issues.GitHubCreateLabelPayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubCreateMilestonePayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubCreatePayload;
import io.github.piotr_janczukowicz.github.api.issues.GitHubCreatePayloadBuilder;
import io.github.piotr_janczukowicz.github.api.issues.GitHubListForRepoFilter;
import io.github.piotr_janczukowicz.github.api.licenses.GitHubLicenseContent;
import io.github.piotr_janczukowicz.github.api.metrics.GitHubCommunityProfile;
import io.github.piotr_janczukowicz.github.api.pages.GitHubCreatePagesSitePayload;
import io.github.piotr_janczukowicz.github.api.pages.GitHubCreatePagesSiteSource;
import io.github.piotr_janczukowicz.github.api.pages.GitHubPagesSourcePath;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubCreatePullRequestPayload;
import io.github.piotr_janczukowicz.github.api.pullrequests.GitHubListPullRequestsFilter;
import io.github.piotr_janczukowicz.github.api.releases.GitHubCreateReleasePayload;
import io.github.piotr_janczukowicz.github.api.releases.GitHubCreateReleasePayloadBuilder;
import io.github.piotr_janczukowicz.github.api.releases.GitHubGenerateReleaseNotesPayload;
import io.github.piotr_janczukowicz.github.api.releases.GitHubReleaseNotesContent;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubContentDirectory;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubContentFile;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubContentSubmodule;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubContentSymlink;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubContributor;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateAutolinkPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateDispatchEventPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateForkPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateOrUpdateFileContentsPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateUsingTemplatePayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubCreateWebhookPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubFullRepository;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubGetContentFilter;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubListForksFilter;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubMinimalRepository;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubPullRequestSimple;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubReplaceAllTopicsPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositorySubscription;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRepositoryTag;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRuleSuite;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubRuleSuiteListItem;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubTransferPayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubUpdatePayload;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubUpdatePayloadBuilder;
import io.github.piotr_janczukowicz.github.api.repositories.GitHubWebhookConfig;
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubListSecretScanningAlertsFilter;
import io.github.piotr_janczukowicz.github.api.secretscanning.GitHubSecretScanningAlert;
import io.github.piotr_janczukowicz.github.api.users.GitHubSimpleUser;
import io.github.piotr_janczukowicz.github.pagination.GitHubPageRequest;
import io.github.piotr_janczukowicz.github.sdk.actions.Artifact;
import io.github.piotr_janczukowicz.github.sdk.actions.RepositoryActionsCache;
import io.github.piotr_janczukowicz.github.sdk.actions.SelfHostedRunners;
import io.github.piotr_janczukowicz.github.sdk.actions.Variable;
import io.github.piotr_janczukowicz.github.sdk.actions.Workflow;
import io.github.piotr_janczukowicz.github.sdk.actions.WorkflowRun;
import io.github.piotr_janczukowicz.github.sdk.advisories.RepositoryAdvisories;
import io.github.piotr_janczukowicz.github.sdk.checks.CheckRun;
import io.github.piotr_janczukowicz.github.sdk.checks.CheckSuite;
import io.github.piotr_janczukowicz.github.sdk.collaborators.Collaborator;
import io.github.piotr_janczukowicz.github.sdk.collaborators.Invitation;
import io.github.piotr_janczukowicz.github.sdk.collaborators.RepositoryPermission;
import io.github.piotr_janczukowicz.github.sdk.content.Autolink;
import io.github.piotr_janczukowicz.github.sdk.content.Branch;
import io.github.piotr_janczukowicz.github.sdk.content.Commit;
import io.github.piotr_janczukowicz.github.sdk.content.CommitComment;
import io.github.piotr_janczukowicz.github.sdk.content.Release;
import io.github.piotr_janczukowicz.github.sdk.content.ReleaseAsset;
import io.github.piotr_janczukowicz.github.sdk.content.RepositoryFile;
import io.github.piotr_janczukowicz.github.sdk.dependencies.RepositoryDependencies;
import io.github.piotr_janczukowicz.github.sdk.deploykeys.DeployKey;
import io.github.piotr_janczukowicz.github.sdk.deployments.Deployment;
import io.github.piotr_janczukowicz.github.sdk.deployments.Environment;
import io.github.piotr_janczukowicz.github.sdk.git.Comparison;
import io.github.piotr_janczukowicz.github.sdk.git.FileChanges;
import io.github.piotr_janczukowicz.github.sdk.git.Ref;
import io.github.piotr_janczukowicz.github.sdk.interactions.RepositoryInteractions;
import io.github.piotr_janczukowicz.github.sdk.internal.Arguments;
import io.github.piotr_janczukowicz.github.sdk.internal.ClientValues;
import io.github.piotr_janczukowicz.github.sdk.internal.Internals;
import io.github.piotr_janczukowicz.github.sdk.internal.Lookups;
import io.github.piotr_janczukowicz.github.sdk.issues.Issue;
import io.github.piotr_janczukowicz.github.sdk.issues.Label;
import io.github.piotr_janczukowicz.github.sdk.issues.Milestone;
import io.github.piotr_janczukowicz.github.sdk.metrics.RepositoryStatistics;
import io.github.piotr_janczukowicz.github.sdk.metrics.RepositoryTraffic;
import io.github.piotr_janczukowicz.github.sdk.notifications.Notification;
import io.github.piotr_janczukowicz.github.sdk.pages.PagesSite;
import io.github.piotr_janczukowicz.github.sdk.pulls.PullRequest;
import io.github.piotr_janczukowicz.github.sdk.rulesets.Ruleset;
import io.github.piotr_janczukowicz.github.sdk.rulesets.RulesetDraft;
import io.github.piotr_janczukowicz.github.sdk.rulesets.RulesetEnforcement;
import io.github.piotr_janczukowicz.github.sdk.rulesets.RulesetTarget;
import io.github.piotr_janczukowicz.github.sdk.security.CodeScanningAlert;
import io.github.piotr_janczukowicz.github.sdk.security.DependabotAlert;
import io.github.piotr_janczukowicz.github.sdk.security.RepositoryCodeQuality;
import io.github.piotr_janczukowicz.github.sdk.security.RepositoryCodeScanning;
import io.github.piotr_janczukowicz.github.sdk.security.RepositorySecurity;
import io.github.piotr_janczukowicz.github.sdk.security.SecretScanningAlert;
import io.github.piotr_janczukowicz.github.sdk.webhooks.Webhook;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A repository, identified by its owner and name. A handle from {@link GitHubSdk#repository(String, String)} holds no
 * data from GitHub; {@link #fetch()} returns a loaded repository. It is the starting point for the operations that
 * belong to a repository.
 */
public final class Repository {

    private static final String PAYLOAD = "payload";
    private static final String BRANCH = "branch";
    private static final String NAME_ARGUMENT = "name";
    private static final String TITLE = "title";
    private static final String FILTER = "filter";
    private static final String LOGIN = "login";
    private static final String VARIABLE_NAME = "variableName";
    private static final String SHA = "sha";
    private static final String REF = "ref";
    private static final String ENVIRONMENT_NAME = "environmentName";
    private static final String IS_GONE = " is gone";

    static {
        Internals.registerRepositoryAccess(repository -> repository.sdk.github());
    }

    private final GitHubSdk sdk;
    private final String owner;
    private final String name;
    private final @Nullable GitHubFullRepository response;

    Repository(
            final GitHubSdk sdk, final String owner, final String name, final @Nullable GitHubFullRepository response) {
        this.sdk = requireNonNull(sdk, "sdk");
        this.owner = requireNotBlank(owner, "owner");
        this.name = requireNotBlank(name, NAME_ARGUMENT);
        this.response = response;
    }

    /**
     * Returns the account that owns the repository.
     *
     * @return the owner login
     */
    public String owner() {
        return owner;
    }

    /**
     * Returns the repository name.
     *
     * @return the name without the owner
     */
    public String name() {
        return name;
    }

    /**
     * Loads the repository ({@code GET /repos/{owner}/{repo}}). This object is not changed. The result carries the
     * owner and name as GitHub spells them, which differ from this handle after a rename or in letter case.
     *
     * @return the loaded repository, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Repository> fetch() throws IOException {
        return sdk.github()
                .repositories()
                .repository()
                .get(owner, name)
                .map(loaded -> new Repository(sdk, loaded.owner().login(), loaded.name(), loaded));
    }

    /**
     * Returns the complete response of a loaded repository.
     *
     * @return the response record
     * @throws IllegalStateException if this is a handle that was not loaded with {@link #fetch()}
     */
    public GitHubFullRepository response() {
        if (response == null) {
            throw new IllegalStateException("Repository " + owner + "/" + name + " is not loaded; call fetch()");
        }
        return response;
    }

    /**
     * Creates an issue with a title ({@code POST /repos/{owner}/{repo}/issues}).
     *
     * @param title the issue title
     * @return the new issue as GitHub returned it
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IOException if the transport fails
     */
    public Issue createIssue(final String title) throws IOException {
        return createIssue(GitHubCreatePayloadBuilder.builder()
                .title(requireNonNull(title, TITLE))
                .build());
    }

    /**
     * Creates an issue ({@code POST /repos/{owner}/{repo}/issues}).
     *
     * @param payload the issue to create
     * @return the new issue as GitHub returned it
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Issue createIssue(final GitHubCreatePayload payload) throws IOException {
        return new Issue(this, sdk.github().issues().issue().create(owner, name, requireNonNull(payload, PAYLOAD)));
    }

    /**
     * Lists the open issues and pull requests ({@code GET /repos/{owner}/{repo}/issues}), the way GitHub lists them by
     * default. See {@link #issues(GitHubListForRepoFilter)}.
     *
     * @return the issues
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Issue> issues() throws IOException {
        return issues(GitHubListForRepoFilter.empty());
    }

    /**
     * Lists issues ({@code GET /repos/{owner}/{repo}/issues}). GitHub also returns pull requests here, as it does for
     * the endpoint. Pages are fetched only as the stream is consumed, so {@code limit} stops further requests;
     * transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the list filter
     * @return the issues
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Issue> issues(final GitHubListForRepoFilter filter) throws IOException {
        return sdk
                .github()
                .issues()
                .issue()
                .listForRepo(owner, name, requireNonNull(filter, FILTER), GitHubPageRequest.byPage())
                .stream()
                .map(listed -> new Issue(this, listed));
    }

    /**
     * Loads one issue of this repository ({@code GET /repos/{owner}/{repo}/issues/{issue_number}}).
     *
     * @param number the issue number
     * @return the issue, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Issue> issue(final int number) throws IOException {
        return sdk.github().issues().issue().get(owner, name, number).map(loaded -> new Issue(this, loaded));
    }

    /**
     * Loads one pull request of this repository ({@code GET /repos/{owner}/{repo}/pulls/{pull_number}}).
     *
     * @param number the pull request number
     * @return the pull request, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<PullRequest> pullRequest(final int number) throws IOException {
        return sdk.github()
                .pullRequests()
                .pullRequest()
                .get(owner, name, number)
                .map(loaded -> new PullRequest(this, loaded));
    }

    /**
     * Lists the open pull requests ({@code GET /repos/{owner}/{repo}/pulls}), the way GitHub lists them by default. See
     * {@link #pullRequests(GitHubListPullRequestsFilter)}.
     *
     * @return the pull requests
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<PullRequest> pullRequests() throws IOException {
        return pullRequests(GitHubListPullRequestsFilter.empty());
    }

    /**
     * Lists pull requests ({@code GET /repos/{owner}/{repo}/pulls}). GitHub's list holds only a summary of each pull
     * request, so every listed pull request is loaded with one more request as the stream reaches it; a {@code limit}
     * on the stream bounds the cost. A transport failure during iteration is an {@link java.io.UncheckedIOException},
     * and a listed pull request that is gone before it is loaded is skipped.
     *
     * @param filter the list filter
     * @return the pull requests
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<PullRequest> pullRequests(final GitHubListPullRequestsFilter filter) throws IOException {
        return sdk
                .github()
                .pullRequests()
                .pullRequest()
                .list(owner, name, requireNonNull(filter, FILTER), GitHubPageRequest.byPage())
                .stream()
                .map(this::loadPullRequest)
                .flatMap(Optional::stream);
    }

    /**
     * Creates a pull request ({@code POST /repos/{owner}/{repo}/pulls}).
     *
     * @param payload the pull request to create
     * @return the new pull request as GitHub returned it
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public PullRequest createPullRequest(final GitHubCreatePullRequestPayload payload) throws IOException {
        return new PullRequest(
                this, sdk.github().pullRequests().pullRequest().create(owner, name, requireNonNull(payload, PAYLOAD)));
    }

    /**
     * Loads one branch ({@code GET /repos/{owner}/{repo}/branches/{branch}}).
     *
     * @param name the branch name
     * @return the branch, or empty when GitHub answers HTTP 404
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IOException if the transport fails
     */
    public Optional<Branch> branch(final String name) throws IOException {
        return sdk.github()
                .branches()
                .branch()
                .getBranch(owner, this.name, Arguments.requireSafePath(name, NAME_ARGUMENT))
                .map(loaded -> new Branch(this, loaded));
    }

    /**
     * Lists the branches ({@code GET /repos/{owner}/{repo}/branches}). See {@link #branches(GitHubListBranchesFilter)}.
     *
     * @return the branches
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Branch> branches() throws IOException {
        return branches(GitHubListBranchesFilter.empty());
    }

    /**
     * Lists branches ({@code GET /repos/{owner}/{repo}/branches}). GitHub's list holds only a summary of each branch,
     * so every listed branch is loaded with one more request as the stream reaches it; a {@code limit} on the stream
     * bounds the cost. A transport failure during iteration is an {@link java.io.UncheckedIOException}, and a listed
     * branch that is gone before it is loaded is skipped.
     *
     * @param filter the list filter
     * @return the branches
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Branch> branches(final GitHubListBranchesFilter filter) throws IOException {
        return sdk
                .github()
                .branches()
                .branch()
                .listBranches(owner, name, requireNonNull(filter, FILTER), GitHubPageRequest.byPage())
                .stream()
                .map(this::loadBranch)
                .flatMap(Optional::stream);
    }

    /**
     * Loads one commit ({@code GET /repos/{owner}/{repo}/commits/{ref}}).
     *
     * @param sha the commit hash, a branch name or a tag name
     * @return the commit, or empty when GitHub answers HTTP 404
     * @throws IllegalArgumentException if {@code sha} is blank, has a control character or has a {@code .} or
     *     {@code ..} segment
     * @throws NullPointerException if {@code sha} is {@code null}
     * @throws IOException if the transport fails
     */
    public Optional<Commit> commit(final String sha) throws IOException {
        return sdk.github()
                .commits()
                .commit()
                .getCommit(owner, name, Arguments.requireSafePath(sha, SHA))
                .map(loaded -> new Commit(this, loaded));
    }

    /**
     * Loads one check run ({@code GET /repos/{owner}/{repo}/check-runs/{check_run_id}}).
     *
     * @param id the check run id
     * @return the check run, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<CheckRun> checkRun(final long id) throws IOException {
        return sdk.github().checks().runs().getCheckRun(owner, name, id).map(loaded -> new CheckRun(this, loaded));
    }

    /**
     * Lists the workflows ({@code GET /repos/{owner}/{repo}/actions/workflows}). Pages are fetched only as the stream
     * is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the workflows
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Workflow> workflows() throws IOException {
        return sdk.github().actions().workflows().listRepoWorkflows(owner, name, GitHubPageRequest.byPage()).stream()
                .map(listed -> new Workflow(this, listed));
    }

    /**
     * Loads one workflow ({@code GET /repos/{owner}/{repo}/actions/workflows/{workflow_id}}).
     *
     * @param idOrFileName the workflow id or the file name, for example {@code build.yml}
     * @return the workflow, or empty when GitHub answers HTTP 404
     * @throws NullPointerException if {@code idOrFileName} is {@code null}
     * @throws IllegalArgumentException if {@code idOrFileName} is blank
     * @throws IOException if the transport fails
     */
    public Optional<Workflow> workflow(final String idOrFileName) throws IOException {
        return sdk.github()
                .actions()
                .workflows()
                .getWorkflow(owner, name, requireNotBlank(idOrFileName, "idOrFileName"))
                .map(loaded -> new Workflow(this, loaded));
    }

    /**
     * Lists the workflow runs of all workflows ({@code GET /repos/{owner}/{repo}/actions/runs}). See
     * {@link #workflowRuns(GitHubListWorkflowRunsForRepoFilter)}.
     *
     * @return the runs, newest first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<WorkflowRun> workflowRuns() throws IOException {
        return workflowRuns(GitHubListWorkflowRunsForRepoFilter.empty());
    }

    /**
     * Lists the workflow runs of all workflows ({@code GET /repos/{owner}/{repo}/actions/runs}). Pages are fetched only
     * as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the filter for actor, branch, event, status, creation date and commit
     * @return the runs, newest first
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<WorkflowRun> workflowRuns(final GitHubListWorkflowRunsForRepoFilter filter) throws IOException {
        return sdk
                .github()
                .actions()
                .workflowRuns()
                .listWorkflowRunsForRepo(owner, name, requireNonNull(filter, FILTER), GitHubPageRequest.byPage())
                .stream()
                .map(listed -> new WorkflowRun(this, listed));
    }

    /**
     * Loads one workflow run ({@code GET /repos/{owner}/{repo}/actions/runs/{run_id}}).
     *
     * @param id the run id
     * @return the run, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<WorkflowRun> workflowRun(final long id) throws IOException {
        return sdk.github()
                .actions()
                .workflowRuns()
                .getWorkflowRun(owner, name, id)
                .map(loaded -> new WorkflowRun(this, loaded));
    }

    /**
     * Lists the artifacts of the repository ({@code GET /repos/{owner}/{repo}/actions/artifacts}). Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the artifacts, newest first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Artifact> artifacts() throws IOException {
        return sdk.github().actions().artifacts().listArtifactsForRepo(owner, name, GitHubPageRequest.byPage()).stream()
                .map(listed -> new Artifact(this, listed));
    }

    /**
     * Loads one artifact ({@code GET /repos/{owner}/{repo}/actions/artifacts/{artifact_id}}).
     *
     * @param id the artifact id
     * @return the artifact, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Artifact> artifact(final long id) throws IOException {
        return sdk.github()
                .actions()
                .artifacts()
                .getArtifact(owner, name, id)
                .map(loaded -> new Artifact(this, loaded));
    }

    /**
     * Lists the Actions variables of the repository itself ({@code GET /repos/{owner}/{repo}/actions/variables}). Pages
     * are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the variables
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Variable> variables() throws IOException {
        return sdk.github().actions().variables().listRepoVariables(owner, name, GitHubPageRequest.byPage()).stream()
                .map(listed -> new Variable(this, listed));
    }

    /**
     * Loads one Actions variable ({@code GET /repos/{owner}/{repo}/actions/variables/{name}}).
     *
     * @param variableName the variable name
     * @return the variable, or empty when GitHub answers HTTP 404
     * @throws NullPointerException if {@code variableName} is {@code null}
     * @throws IllegalArgumentException if {@code variableName} is blank
     * @throws IOException if the transport fails
     */
    public Optional<Variable> variable(final String variableName) throws IOException {
        return sdk.github()
                .actions()
                .variables()
                .getRepoVariable(owner, name, requireNotBlank(variableName, VARIABLE_NAME))
                .map(loaded -> new Variable(this, loaded));
    }

    /**
     * Creates an Actions variable ({@code POST /repos/{owner}/{repo}/actions/variables}).
     *
     * @param variableName the variable name
     * @param value the value
     * @return the variable as GitHub holds it afterwards
     * @throws NullPointerException if {@code variableName} or {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code variableName} is blank
     * @throws IOException if the transport fails
     * @throws IllegalStateException if GitHub does not have the variable it just created
     */
    public Variable createVariable(final String variableName, final String value) throws IOException {
        final String validName = requireNotBlank(variableName, VARIABLE_NAME);
        sdk.github()
                .actions()
                .variables()
                .createRepoVariable(
                        owner, name, new GitHubCreateRepoVariablePayload(validName, requireNonNull(value, "value")));
        return variable(validName).orElseThrow(() -> new IllegalStateException("Variable " + validName + IS_GONE));
    }

    /**
     * Lists the Actions secrets of the repository itself ({@code GET /repos/{owner}/{repo}/actions/secrets}). GitHub
     * never returns a secret value, only the name and the dates. Creating a secret needs the value encrypted with the
     * repository public key, which this library does not do; use the client for that. Pages are fetched only as the
     * stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the secrets
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubActionsSecret> secrets() throws IOException {
        return sdk.github().actions().secrets().listRepoSecrets(owner, name, GitHubPageRequest.byPage()).stream();
    }

    /**
     * Deletes an Actions secret ({@code DELETE /repos/{owner}/{repo}/actions/secrets/{secret_name}}).
     *
     * @param secretName the secret name
     * @throws NullPointerException if {@code secretName} is {@code null}
     * @throws IllegalArgumentException if {@code secretName} is blank
     * @throws IOException if the transport fails
     */
    public void deleteSecret(final String secretName) throws IOException {
        sdk.github().actions().secrets().deleteRepoSecret(owner, name, requireNotBlank(secretName, "secretName"));
    }

    /**
     * Lists the commits of the default branch ({@code GET /repos/{owner}/{repo}/commits}). See
     * {@link #commits(GitHubListCommitsFilter)}.
     *
     * @return the commits, newest first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Commit> commits() throws IOException {
        return commits(GitHubListCommitsFilter.empty());
    }

    /**
     * Lists commits ({@code GET /repos/{owner}/{repo}/commits}). Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the list filter
     * @return the commits, newest first
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Commit> commits(final GitHubListCommitsFilter filter) throws IOException {
        return sdk
                .github()
                .commits()
                .commit()
                .listCommits(owner, name, requireNonNull(filter, FILTER), GitHubPageRequest.byPage())
                .stream()
                .map(listed -> new Commit(this, listed));
    }

    /**
     * Loads one file on the default branch ({@code GET /repos/{owner}/{repo}/contents/{path}}).
     *
     * @param path the path within the repository
     * @return the file, or empty when GitHub answers HTTP 404 or the path is a directory, symbolic link or submodule
     * @throws IllegalArgumentException if {@code path} is blank, has a control character or has a {@code .} or
     *     {@code ..} segment
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws IOException if the transport fails
     */
    public Optional<RepositoryFile> file(final String path) throws IOException {
        return read(Arguments.requireSafePath(path, "path"), null);
    }

    /**
     * Loads one file at a ref ({@code GET /repos/{owner}/{repo}/contents/{path}}).
     *
     * @param path the path within the repository
     * @param ref the branch, tag or commit
     * @return the file, or empty when GitHub answers HTTP 404 or the path is a directory, symbolic link or submodule
     * @throws IllegalArgumentException if {@code path} or {@code ref} is blank
     * @throws NullPointerException if {@code path} or {@code ref} is {@code null}
     * @throws IOException if the transport fails
     */
    public Optional<RepositoryFile> file(final String path, final String ref) throws IOException {
        return read(Arguments.requireSafePath(path, "path"), Arguments.requireSafePath(ref, REF));
    }

    /**
     * Creates or replaces a file on the default branch ({@code PUT /repos/{owner}/{repo}/contents/{path}}) and loads it
     * again. An existing file is replaced using its current blob hash.
     *
     * @param path the path within the repository
     * @param message the commit message
     * @param content the new content
     * @return the file as GitHub holds it after the write
     * @throws NullPointerException if {@code content} is {@code null}
     * @throws IllegalArgumentException if {@code path} or {@code message} is blank
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the file is gone after the write
     */
    public RepositoryFile writeFile(final String path, final String message, final byte[] content) throws IOException {
        return write(path, message, content, null);
    }

    /**
     * Creates or replaces a file on a branch ({@code PUT /repos/{owner}/{repo}/contents/{path}}) and loads it again. An
     * existing file is replaced using its current blob hash on that branch.
     *
     * @param path the path within the repository
     * @param message the commit message
     * @param content the new content
     * @param branch the branch to commit to
     * @return the file as GitHub holds it after the write
     * @throws NullPointerException if {@code content} is {@code null}
     * @throws IllegalArgumentException if {@code path}, {@code message} or {@code branch} is blank
     * @throws IOException if the transport fails
     * @throws IllegalStateException if the file is gone after the write
     */
    public RepositoryFile writeFile(final String path, final String message, final byte[] content, final String branch)
            throws IOException {
        return write(path, message, content, Arguments.requireSafePath(branch, BRANCH));
    }

    /**
     * Loads the release built on a tag ({@code GET /repos/{owner}/{repo}/releases/tags/{tag}}).
     *
     * @param tag the tag name
     * @return the release, or empty when GitHub answers HTTP 404
     * @throws IllegalArgumentException if {@code tag} is blank
     * @throws NullPointerException if {@code tag} is {@code null}
     * @throws IOException if the transport fails
     */
    public Optional<Release> release(final String tag) throws IOException {
        return sdk.github()
                .releases()
                .release()
                .getReleaseByTag(owner, name, requireNotBlank(tag, "tag"))
                .map(loaded -> new Release(this, loaded));
    }

    /**
     * Loads the latest published release ({@code GET /repos/{owner}/{repo}/releases/latest}).
     *
     * @return the release, or empty when the repository has none
     * @throws IOException if the transport fails
     */
    public Optional<Release> latestRelease() throws IOException {
        return sdk.github().releases().release().getLatestRelease(owner, name).map(loaded -> new Release(this, loaded));
    }

    /**
     * Lists the releases ({@code GET /repos/{owner}/{repo}/releases}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the releases, newest first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Release> releases() throws IOException {
        return sdk.github().releases().release().listReleases(owner, name).stream()
                .map(listed -> new Release(this, listed));
    }

    /**
     * Creates a release on a tag ({@code POST /repos/{owner}/{repo}/releases}). GitHub creates the tag from the default
     * branch when it does not exist.
     *
     * @param tagName the tag name
     * @return the new release as GitHub returned it
     * @throws IllegalArgumentException if {@code tagName} is blank
     * @throws NullPointerException if {@code tagName} is {@code null}
     * @throws IOException if the transport fails
     */
    public Release createRelease(final String tagName) throws IOException {
        return createRelease(GitHubCreateReleasePayloadBuilder.builder()
                .tagName(requireNotBlank(tagName, "tagName"))
                .build());
    }

    /**
     * Creates a release ({@code POST /repos/{owner}/{repo}/releases}).
     *
     * @param payload the release to create
     * @return the new release as GitHub returned it
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Release createRelease(final GitHubCreateReleasePayload payload) throws IOException {
        return new Release(
                this, sdk.github().releases().release().createRelease(owner, name, requireNonNull(payload, PAYLOAD)));
    }

    /**
     * Loads a release asset by its id ({@code GET /repos/{owner}/{repo}/releases/assets/{asset_id}}). Asset ids are
     * unique in the repository, so no release is needed.
     *
     * @param assetId the asset id
     * @return the asset, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<ReleaseAsset> releaseAsset(final long assetId) throws IOException {
        return sdk.github()
                .releases()
                .assets()
                .getReleaseAsset(owner, name, assetId)
                .map(loaded -> new ReleaseAsset(this, loaded));
    }

    /**
     * Generates the notes for a release on a tag ({@code POST /repos/{owner}/{repo}/releases/generate-notes}) from the
     * merged pull requests. Nothing is created: pass the text to {@link #createRelease(GitHubCreateReleasePayload)}.
     *
     * @param tagName the tag the notes are for; it need not exist yet
     * @return the generated name and body as GitHub returns them
     * @throws NullPointerException if {@code tagName} is {@code null}
     * @throws IllegalArgumentException if {@code tagName} is blank
     * @throws IOException if the transport fails
     */
    public GitHubReleaseNotesContent generateReleaseNotes(final String tagName) throws IOException {
        return generateReleaseNotes(
                new GitHubGenerateReleaseNotesPayload(requireNotBlank(tagName, "tagName"), null, null, null));
    }

    /**
     * Generates the notes for a release with every option the client offers, such as the previous tag ({@code POST
     * /repos/{owner}/{repo}/releases/generate-notes}). Nothing is created.
     *
     * @param payload the tag and options
     * @return the generated name and body as GitHub returns them
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public GitHubReleaseNotesContent generateReleaseNotes(final GitHubGenerateReleaseNotesPayload payload)
            throws IOException {
        return sdk.github().releases().release().generateReleaseNotes(owner, name, requireNonNull(payload, PAYLOAD));
    }

    /**
     * Lists the tags ({@code GET /repos/{owner}/{repo}/tags}). Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the tags
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubRepositoryTag> tags() throws IOException {
        return sdk.github().repositories().repository().listTags(owner, name).stream();
    }

    /**
     * Loads one label ({@code GET /repos/{owner}/{repo}/labels/{name}}).
     *
     * @param labelName the label name
     * @return the label, or empty when GitHub answers HTTP 404
     * @throws IllegalArgumentException if {@code labelName} is blank
     * @throws NullPointerException if {@code labelName} is {@code null}
     * @throws IOException if the transport fails
     */
    public Optional<Label> label(final String labelName) throws IOException {
        return sdk.github()
                .issues()
                .labels()
                .getLabel(owner, name, requireNotBlank(labelName, NAME_ARGUMENT))
                .map(loaded -> new Label(this, loaded));
    }

    /**
     * Lists the labels ({@code GET /repos/{owner}/{repo}/labels}). Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the labels
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Label> labels() throws IOException {
        return sdk.github().issues().labels().listLabelsForRepo(owner, name).stream()
                .map(listed -> new Label(this, listed));
    }

    /**
     * Creates a label ({@code POST /repos/{owner}/{repo}/labels}).
     *
     * @param labelName the label name
     * @param color the color as a hexadecimal value without the leading {@code #}
     * @return the new label as GitHub returned it
     * @throws IllegalArgumentException if {@code labelName} or {@code color} is blank
     * @throws NullPointerException if {@code labelName} or {@code color} is {@code null}
     * @throws IOException if the transport fails
     */
    public Label createLabel(final String labelName, final String color) throws IOException {
        return createLabel(new GitHubCreateLabelPayload(
                requireNotBlank(labelName, NAME_ARGUMENT), requireNotBlank(color, "color"), null));
    }

    /**
     * Creates a label ({@code POST /repos/{owner}/{repo}/labels}).
     *
     * @param payload the label to create
     * @return the new label as GitHub returned it
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Label createLabel(final GitHubCreateLabelPayload payload) throws IOException {
        return new Label(
                this, sdk.github().issues().labels().createLabel(owner, name, requireNonNull(payload, PAYLOAD)));
    }

    /**
     * Loads one milestone ({@code GET /repos/{owner}/{repo}/milestones/{milestone_number}}).
     *
     * @param number the milestone number
     * @return the milestone, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Milestone> milestone(final int number) throws IOException {
        return sdk.github()
                .issues()
                .milestones()
                .getMilestone(owner, name, number)
                .map(loaded -> new Milestone(this, loaded));
    }

    /**
     * Lists the milestones ({@code GET /repos/{owner}/{repo}/milestones}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the milestones
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Milestone> milestones() throws IOException {
        return sdk.github().issues().milestones().listMilestones(owner, name).stream()
                .map(listed -> new Milestone(this, listed));
    }

    /**
     * Creates a milestone ({@code POST /repos/{owner}/{repo}/milestones}).
     *
     * @param title the milestone title
     * @return the new milestone as GitHub returned it
     * @throws IllegalArgumentException if {@code title} is blank
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IOException if the transport fails
     */
    public Milestone createMilestone(final String title) throws IOException {
        return createMilestone(new GitHubCreateMilestonePayload(requireNotBlank(title, TITLE), null, null, null));
    }

    /**
     * Creates a milestone ({@code POST /repos/{owner}/{repo}/milestones}).
     *
     * @param payload the milestone to create
     * @return the new milestone as GitHub returned it
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Milestone createMilestone(final GitHubCreateMilestonePayload payload) throws IOException {
        return new Milestone(
                this,
                sdk.github().issues().milestones().createMilestone(owner, name, requireNonNull(payload, PAYLOAD)));
    }

    /**
     * Loads one webhook ({@code GET /repos/{owner}/{repo}/hooks/{hook_id}}).
     *
     * @param id the webhook id
     * @return the webhook, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Webhook> webhook(final long id) throws IOException {
        return sdk.github()
                .repositories()
                .webhooks()
                .getWebhook(owner, name, id)
                .map(loaded -> new Webhook(this, loaded));
    }

    /**
     * Lists the webhooks ({@code GET /repos/{owner}/{repo}/hooks}). Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the webhooks
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Webhook> webhooks() throws IOException {
        return sdk.github().repositories().webhooks().listWebhooks(owner, name).stream()
                .map(listed -> new Webhook(this, listed));
    }

    /**
     * Creates a webhook that delivers JSON to a URL ({@code POST /repos/{owner}/{repo}/hooks}).
     *
     * @param payloadUrl the URL GitHub delivers the events to
     * @param events the event names to subscribe to
     * @return the new webhook as GitHub returned it
     * @throws IllegalArgumentException if {@code payloadUrl} or an event name is blank
     * @throws NullPointerException if {@code payloadUrl}, {@code events} or an event name is {@code null}
     * @throws IOException if the transport fails
     */
    public Webhook createWebhook(final String payloadUrl, final List<String> events) throws IOException {
        return createWebhook(new GitHubCreateWebhookPayload(
                "web",
                new GitHubWebhookConfig(requireNotBlank(payloadUrl, "payloadUrl"), "json", null, null),
                Arguments.requireNotBlankElements(events, "events"),
                null));
    }

    /**
     * Creates a webhook ({@code POST /repos/{owner}/{repo}/hooks}).
     *
     * @param payload the webhook to create
     * @return the new webhook as GitHub returned it
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Webhook createWebhook(final GitHubCreateWebhookPayload payload) throws IOException {
        return new Webhook(
                this,
                sdk.github().repositories().webhooks().createWebhook(owner, name, requireNonNull(payload, PAYLOAD)));
    }

    /**
     * Lists the collaborators with their default affiliation ({@code GET /repos/{owner}/{repo}/collaborators}). See
     * {@link #collaborators(GitHubListCollaboratorsFilter)}.
     *
     * @return the collaborators
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Collaborator> collaborators() throws IOException {
        return collaborators(GitHubListCollaboratorsFilter.empty());
    }

    /**
     * Lists the collaborators ({@code GET /repos/{owner}/{repo}/collaborators}). Pages are fetched only as the stream
     * is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the affiliation and permission to list
     * @return the collaborators
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Collaborator> collaborators(final GitHubListCollaboratorsFilter filter) throws IOException {
        return sdk
                .github()
                .collaborators()
                .collaborator()
                .listCollaborators(owner, name, requireNonNull(filter, FILTER), GitHubPageRequest.byPage())
                .stream()
                .map(listed -> new Collaborator(this, listed));
    }

    /**
     * Tells whether a user is a collaborator ({@code GET /repos/{owner}/{repo}/collaborators/{username}}).
     *
     * @param login the user's login
     * @return {@code true} when the user is a collaborator
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws NullPointerException if {@code login} is {@code null}
     * @throws IOException if the transport fails
     */
    public boolean isCollaborator(final String login) throws IOException {
        return sdk.github()
                .collaborators()
                .collaborator()
                .checkCollaborator(owner, name, requireNotBlank(login, LOGIN));
    }

    /**
     * Loads a user's permission level ({@code GET /repos/{owner}/{repo}/collaborators/{username}/permission}). The
     * client's record is passed through.
     *
     * @param login the user's login
     * @return the permission level, or empty when GitHub answers HTTP 404
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws NullPointerException if {@code login} is {@code null}
     * @throws IOException if the transport fails
     */
    public Optional<GitHubRepositoryCollaboratorPermission> collaboratorPermission(final String login)
            throws IOException {
        return sdk.github()
                .collaborators()
                .collaborator()
                .getCollaboratorPermissionLevel(owner, name, requireNotBlank(login, LOGIN));
    }

    /**
     * Invites a user with the permission the repository's organization gives by default ({@code PUT
     * /repos/{owner}/{repo}/collaborators/{username}}). GitHub emails the user.
     *
     * @param login the user's login
     * @return the invitation, or empty when GitHub added the user without one
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws NullPointerException if {@code login} is {@code null}
     * @throws IOException if the transport fails
     */
    public Optional<Invitation> addCollaborator(final String login) throws IOException {
        return sdk.github()
                .collaborators()
                .collaborator()
                .addCollaborator(owner, name, requireNotBlank(login, LOGIN))
                .map(invited -> new Invitation(this, invited));
    }

    /**
     * Invites a user ({@code PUT /repos/{owner}/{repo}/collaborators/{username}}). GitHub emails the user.
     *
     * @param login the user's login
     * @param permission the access to grant
     * @return the invitation, or empty when GitHub added the user without one
     * @throws IllegalArgumentException if {@code login} is blank
     * @throws NullPointerException if {@code permission} is {@code null}
     * @throws IOException if the transport fails
     */
    public Optional<Invitation> addCollaborator(final String login, final RepositoryPermission permission)
            throws IOException {
        final GitHubAddCollaboratorPayload payload = new GitHubAddCollaboratorPayload(
                ClientValues.permissionToClient(requireNonNull(permission, "permission"))
                        .wireValue());
        return sdk.github()
                .collaborators()
                .collaborator()
                .addCollaborator(owner, name, requireNotBlank(login, LOGIN), payload)
                .map(invited -> new Invitation(this, invited));
    }

    /**
     * Lists the pending invitations ({@code GET /repos/{owner}/{repo}/invitations}). Pages are fetched only as the
     * stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the invitations
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Invitation> invitations() throws IOException {
        return sdk.github().collaborators().invitations().listInvitations(owner, name).stream()
                .map(listed -> new Invitation(this, listed));
    }

    /**
     * Stars the repository for the authenticated user ({@code PUT /user/starred/{owner}/{repo}}). Starring a starred
     * repository changes nothing.
     *
     * @throws IOException if the transport fails
     */
    public void star() throws IOException {
        sdk.github().activity().starring().starRepoForAuthenticatedUser(owner, name);
    }

    /**
     * Removes the authenticated user's star ({@code DELETE /user/starred/{owner}/{repo}}). Unstarring a repository that
     * is not starred changes nothing.
     *
     * @throws IOException if the transport fails
     */
    public void unstar() throws IOException {
        sdk.github().activity().starring().unstarRepoForAuthenticatedUser(owner, name);
    }

    /**
     * Tells whether the authenticated user starred the repository ({@code GET /user/starred/{owner}/{repo}}).
     *
     * @return {@code true} when the repository is starred
     * @throws IOException if the transport fails
     */
    public boolean isStarred() throws IOException {
        return sdk.github().activity().starring().checkRepoIsStarredByAuthenticatedUser(owner, name);
    }

    /**
     * Lists the users who starred the repository ({@code GET /repos/{owner}/{repo}/stargazers}). The client's records
     * are passed through. Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the stargazers
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubSimpleUser> stargazers() throws IOException {
        return sdk.github().activity().starring().listStargazersForRepo(owner, name).stream();
    }

    /**
     * Lists the users who watch the repository ({@code GET /repos/{owner}/{repo}/subscribers}). The client's records
     * are passed through. Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the watchers
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubSimpleUser> watchers() throws IOException {
        return sdk.github().activity().watching().listWatchersForRepo(owner, name).stream();
    }

    /**
     * Loads the authenticated user's subscription to the repository ({@code GET /repos/{owner}/{repo}/subscription}).
     * The client's record is passed through.
     *
     * @return the subscription, or empty when the user neither watches nor ignores the repository
     * @throws IOException if the transport fails
     */
    public Optional<GitHubRepositorySubscription> subscription() throws IOException {
        return sdk.github().activity().watching().getRepoSubscription(owner, name);
    }

    /**
     * Subscribes the authenticated user to all notifications of the repository ({@code PUT
     * /repos/{owner}/{repo}/subscription}).
     *
     * @return the subscription as GitHub returned it
     * @throws IOException if the transport fails
     */
    public GitHubRepositorySubscription watch() throws IOException {
        return sdk.github()
                .activity()
                .watching()
                .setRepoSubscription(owner, name, new GitHubSetRepoSubscriptionPayload(true, false));
    }

    /**
     * Mutes all notifications of the repository for the authenticated user ({@code PUT
     * /repos/{owner}/{repo}/subscription}).
     *
     * @return the subscription as GitHub returned it
     * @throws IOException if the transport fails
     */
    public GitHubRepositorySubscription ignore() throws IOException {
        return sdk.github()
                .activity()
                .watching()
                .setRepoSubscription(owner, name, new GitHubSetRepoSubscriptionPayload(false, true));
    }

    /**
     * Removes the authenticated user's subscription ({@code DELETE /repos/{owner}/{repo}/subscription}), so the user is
     * notified only about conversations they take part in.
     *
     * @throws IOException if the transport fails
     */
    public void unwatch() throws IOException {
        sdk.github().activity().watching().deleteRepoSubscription(owner, name);
    }

    /**
     * Lists the forks ({@code GET /repos/{owner}/{repo}/forks}). See {@link #forks(GitHubListForksFilter)}.
     *
     * @return handles of the forks
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Repository> forks() throws IOException {
        return forks(GitHubListForksFilter.empty());
    }

    /**
     * Lists the forks ({@code GET /repos/{owner}/{repo}/forks}). The elements are handles that are not loaded; call
     * {@link #fetch()} to load one. Pages are fetched only as the stream is consumed; transport failures during
     * iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the sort order
     * @return handles of the forks
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Repository> forks(final GitHubListForksFilter filter) throws IOException {
        return sdk
                .github()
                .repositories()
                .forks()
                .listForks(owner, name, requireNonNull(filter, FILTER), GitHubPageRequest.byPage())
                .stream()
                .map(listed -> sdk.repository(listed.owner().login(), listed.name()));
    }

    /**
     * Forks the repository into the authenticated user's account ({@code POST /repos/{owner}/{repo}/forks}). GitHub
     * creates the fork asynchronously, so the Git data may not be available right away.
     *
     * @return the fork, loaded with what GitHub returned
     * @throws IOException if the transport fails
     */
    public Repository fork() throws IOException {
        return createFork(null);
    }

    /**
     * Forks the repository with options ({@code POST /repos/{owner}/{repo}/forks}). GitHub creates the fork
     * asynchronously, so the Git data may not be available right away.
     *
     * @param payload the target organization, the fork's name and whether to copy only the default branch
     * @return the fork, loaded with what GitHub returned
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Repository fork(final GitHubCreateForkPayload payload) throws IOException {
        return createFork(requireNonNull(payload, PAYLOAD));
    }

    /**
     * Starts a change of several settings at once; nothing is sent until {@link RepositoryUpdate#send()}.
     *
     * @return a new update for this repository
     */
    public RepositoryUpdate update() {
        return new RepositoryUpdate(this);
    }

    /**
     * Archives the repository ({@code PATCH /repos/{owner}/{repo}}), which makes it read-only. GitHub's API has no way
     * to undo this; only the web interface can unarchive a repository. This object is not changed.
     *
     * @return the repository as GitHub returned it after the change
     * @throws IOException if the transport fails
     */
    public Repository archive() throws IOException {
        return apply(GitHubUpdatePayloadBuilder.builder().archived(true).build());
    }

    /**
     * Deletes the repository ({@code DELETE /repos/{owner}/{repo}}). This cannot be undone.
     *
     * @throws IOException if the transport fails
     */
    public void delete() throws IOException {
        sdk.github().repositories().repository().delete(owner, name);
    }

    /**
     * Transfers the repository to another owner and keeps its name. See {@link #transfer(String, String)}.
     *
     * @param newOwner the login of the user or organization that receives the repository
     * @return a handle to the repository under its new owner
     * @throws NullPointerException if {@code newOwner} is {@code null}
     * @throws IllegalArgumentException if {@code newOwner} is blank
     * @throws IOException if the transport fails
     */
    public Repository transfer(final String newOwner) throws IOException {
        return transferTo(newOwner, null);
    }

    /**
     * Transfers the repository to another owner and renames it ({@code POST /repos/{owner}/{repo}/transfer}). GitHub
     * completes the transfer asynchronously, and a transfer to a user needs that user to accept it, so the returned
     * handle may not be usable at once. This object is not changed.
     *
     * @param newOwner the login of the user or organization that receives the repository
     * @param newName the new name of the repository
     * @return a handle to the repository under its new owner
     * @throws NullPointerException if {@code newOwner} or {@code newName} is {@code null}
     * @throws IllegalArgumentException if {@code newOwner} or {@code newName} is blank
     * @throws IOException if the transport fails
     */
    public Repository transfer(final String newOwner, final String newName) throws IOException {
        return transferTo(newOwner, requireNotBlank(newName, "newName"));
    }

    /**
     * Lists the topics ({@code GET /repos/{owner}/{repo}/topics}).
     *
     * @return the topic names
     * @throws IOException if the transport fails
     */
    public List<String> topics() throws IOException {
        return sdk.github()
                .repositories()
                .repository()
                .getAllTopics(owner, name)
                .names();
    }

    /**
     * Replaces all topics ({@code PUT /repos/{owner}/{repo}/topics}). An empty list removes them all.
     *
     * @param topics the topic names
     * @return the topics as GitHub holds them afterwards
     * @throws NullPointerException if {@code topics} or a topic is {@code null}
     * @throws IllegalArgumentException if a topic is blank
     * @throws IOException if the transport fails
     */
    public List<String> replaceTopics(final List<String> topics) throws IOException {
        return sdk.github()
                .repositories()
                .repository()
                .replaceAllTopics(
                        owner,
                        name,
                        new GitHubReplaceAllTopicsPayload(Arguments.requireNotBlankElements(topics, "topics")))
                .names();
    }

    /**
     * Lists the languages of the repository ({@code GET /repos/{owner}/{repo}/languages}).
     *
     * @return a map from each language name to the number of bytes of code written in it
     * @throws IOException if the transport fails
     */
    public Map<String, Integer> languages() throws IOException {
        return sdk.github()
                .repositories()
                .repository()
                .listLanguages(owner, name)
                .languages();
    }

    /**
     * Lists the contributors ({@code GET /repos/{owner}/{repo}/contributors}) as GitHub returns them. Pages are fetched
     * only as the stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the contributors, most commits first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubContributor> contributors() throws IOException {
        return sdk
                .github()
                .repositories()
                .repository()
                .listContributors(owner, name, GitHubPageRequest.byPage())
                .stream();
    }

    /**
     * Triggers workflows that listen to a {@code repository_dispatch} event. See {@link #dispatchEvent(String, Map)}.
     *
     * @param eventType the custom event type
     * @throws NullPointerException if {@code eventType} is {@code null}
     * @throws IllegalArgumentException if {@code eventType} is blank
     * @throws IOException if the transport fails
     */
    public void dispatchEvent(final String eventType) throws IOException {
        dispatch(eventType, null);
    }

    /**
     * Triggers workflows that listen to a {@code repository_dispatch} event ({@code POST
     * /repos/{owner}/{repo}/dispatches}). GitHub answers with no body, so no run is returned.
     *
     * @param eventType the custom event type
     * @param clientPayload the data the workflows read from {@code github.event.client_payload}; values may be
     *     {@code null}, which is sent as JSON null
     * @throws NullPointerException if {@code eventType} or {@code clientPayload} is {@code null}
     * @throws IllegalArgumentException if {@code eventType} is blank
     * @throws IOException if the transport fails
     */
    public void dispatchEvent(final String eventType, final Map<String, Object> clientPayload) throws IOException {
        dispatch(
                eventType,
                Collections.unmodifiableMap(new LinkedHashMap<>(requireNonNull(clientPayload, "clientPayload"))));
    }

    /**
     * Creates a new repository from this one, which must be a template. See
     * {@link #generate(GitHubCreateUsingTemplatePayload)}.
     *
     * @param newOwner the owner of the new repository
     * @param newName the name of the new repository
     * @return the new repository
     * @throws NullPointerException if {@code newOwner} or {@code newName} is {@code null}
     * @throws IllegalArgumentException if {@code newOwner} or {@code newName} is blank
     * @throws IOException if the transport fails
     */
    public Repository generate(final String newOwner, final String newName) throws IOException {
        return generate(new GitHubCreateUsingTemplatePayload(
                requireNotBlank(newOwner, "newOwner"), requireNotBlank(newName, "newName"), null, null, null));
    }

    /**
     * Creates a new repository from this one, which must be a template ({@code POST
     * /repos/{template_owner}/{template_repo}/generate}).
     *
     * @param payload the owner, name and options of the new repository
     * @return the new repository
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public Repository generate(final GitHubCreateUsingTemplatePayload payload) throws IOException {
        final GitHubFullRepository created = sdk.github()
                .repositories()
                .repository()
                .createUsingTemplate(owner, name, requireNonNull(payload, PAYLOAD));
        return new Repository(sdk, created.owner().login(), created.name(), created);
    }

    /**
     * Starts a commit of several file changes to a branch; nothing is sent until {@link FileChanges#send(String)}.
     *
     * @param branch the name of the branch that receives the commit
     * @return a new, empty set of changes
     * @throws NullPointerException if {@code branch} is {@code null}
     * @throws IllegalArgumentException if {@code branch} is blank, has a control character or has a {@code .} or
     *     {@code ..} segment
     */
    public FileChanges commitTo(final String branch) {
        return new FileChanges(this, branch);
    }

    /**
     * Compares two commits, branches or tags ({@code GET /repos/{owner}/{repo}/compare/{base}...{head}}).
     *
     * @param base the base, such as a branch name or a sha
     * @param head the head, such as a branch name or a sha
     * @return the comparison, or empty when GitHub answers HTTP 404
     * @throws NullPointerException if {@code base} or {@code head} is {@code null}
     * @throws IllegalArgumentException if {@code base} or {@code head} is blank
     * @throws IOException if the transport fails
     */
    public Optional<Comparison> compare(final String base, final String head) throws IOException {
        final String baseHead =
                Arguments.requireSafePath(base, "base") + "..." + Arguments.requireSafePath(head, "head");
        return sdk.github()
                .commits()
                .commit()
                .compareCommits(owner, name, baseHead)
                .map(loaded -> new Comparison(this, baseHead, loaded));
    }

    /**
     * Lists the references that start with a prefix ({@code GET /repos/{owner}/{repo}/git/matching-refs/{ref}}). Pages
     * are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @param prefix the start of the reference name without {@code refs/}, such as {@code heads/} or {@code tags/}
     * @return the references
     * @throws NullPointerException if {@code prefix} is {@code null}
     * @throws IllegalArgumentException if {@code prefix} is blank, has a control character, or has a {@code .} or
     *     {@code ..} segment
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Ref> refs(final String prefix) throws IOException {
        return sdk
                .github()
                .gitDatabase()
                .refs()
                .listMatchingRefs(owner, name, Arguments.requireSafePath(prefix, "prefix"))
                .stream()
                .map(listed -> new Ref(this, listed));
    }

    /**
     * Loads one reference ({@code GET /repos/{owner}/{repo}/git/ref/{ref}}).
     *
     * @param ref the reference name without {@code refs/}, such as {@code heads/main} or {@code tags/v1}
     * @return the reference, or empty when GitHub answers HTTP 404
     * @throws NullPointerException if {@code ref} is {@code null}
     * @throws IllegalArgumentException if {@code ref} is blank, has a control character or has a {@code .} or
     *     {@code ..} segment
     * @throws IOException if the transport fails
     */
    public Optional<Ref> ref(final String ref) throws IOException {
        return sdk.github()
                .gitDatabase()
                .refs()
                .getRef(owner, name, Arguments.requireSafePath(ref, REF))
                .map(loaded -> new Ref(this, loaded));
    }

    /**
     * Creates a reference ({@code POST /repos/{owner}/{repo}/git/refs}).
     *
     * @param ref the full reference name, such as {@code refs/heads/feature} or {@code refs/tags/v1}
     * @param sha the sha of the object it points to
     * @return the new reference
     * @throws NullPointerException if {@code ref} or {@code sha} is {@code null}
     * @throws IllegalArgumentException if {@code ref} or {@code sha} is blank
     * @throws IOException if the transport fails
     */
    public Ref createRef(final String ref, final String sha) throws IOException {
        return new Ref(
                this,
                sdk.github()
                        .gitDatabase()
                        .refs()
                        .createRef(
                                owner,
                                name,
                                new GitHubCreateRefPayload(
                                        Arguments.requireSafePath(ref, REF), Arguments.requireSafePath(sha, SHA))));
    }

    /**
     * Creates a branch that starts at a commit. See {@link #createRef(String, String)}.
     *
     * @param branch the name of the new branch
     * @param sha the sha of the commit it starts at
     * @return the new branch
     * @throws NullPointerException if {@code branch} or {@code sha} is {@code null}
     * @throws IllegalArgumentException if {@code branch} or {@code sha} is blank
     * @throws IOException if the transport fails
     * @throws IllegalStateException if GitHub does not have the branch it just created
     */
    public Branch createBranch(final String branch, final String sha) throws IOException {
        final String validBranch = Arguments.requireSafePath(branch, BRANCH);
        createRef("refs/heads/" + validBranch, sha);
        return branch(validBranch).orElseThrow(() -> new IllegalStateException("Branch " + validBranch + IS_GONE));
    }

    /**
     * Loads a git tree ({@code GET /repos/{owner}/{repo}/git/trees/{tree_sha}}) as GitHub returns it, with the direct
     * entries only. See {@link #tree(String, boolean)}.
     *
     * @param sha the sha of the tree, or of a commit or branch that points to it
     * @return the tree, or empty when GitHub answers HTTP 404
     * @throws NullPointerException if {@code sha} is {@code null}
     * @throws IllegalArgumentException if {@code sha} is blank, has a control character or has a {@code .} or
     *     {@code ..} segment
     * @throws IOException if the transport fails
     */
    public Optional<GitHubGitTree> tree(final String sha) throws IOException {
        return tree(sha, false);
    }

    /**
     * Loads a git tree ({@code GET /repos/{owner}/{repo}/git/trees/{tree_sha}}) as GitHub returns it. GitHub cuts a
     * very large tree off and says so in {@code truncated()}.
     *
     * @param sha the sha of the tree, or of a commit or branch that points to it
     * @param isRecursive {@code true} to include the entries of all subdirectories
     * @return the tree, or empty when GitHub answers HTTP 404
     * @throws NullPointerException if {@code sha} is {@code null}
     * @throws IllegalArgumentException if {@code sha} is blank, has a control character or has a {@code .} or
     *     {@code ..} segment
     * @throws IOException if the transport fails
     */
    public Optional<GitHubGitTree> tree(final String sha, final boolean isRecursive) throws IOException {
        return sdk.github()
                .gitDatabase()
                .trees()
                .getTree(
                        owner,
                        name,
                        Arguments.requireSafePath(sha, SHA),
                        isRecursive ? GitHubGetTreeFilter.recursively() : GitHubGetTreeFilter.empty());
    }

    /**
     * Loads a git blob ({@code GET /repos/{owner}/{repo}/git/blobs/{file_sha}}) as GitHub returns it.
     *
     * @param sha the sha of the blob
     * @return the blob, or empty when GitHub answers HTTP 404
     * @throws NullPointerException if {@code sha} is {@code null}
     * @throws IllegalArgumentException if {@code sha} is blank, has a control character or has a {@code .} or
     *     {@code ..} segment
     * @throws IOException if the transport fails
     */
    public Optional<GitHubBlob> blob(final String sha) throws IOException {
        return sdk.github().gitDatabase().blobs().getBlob(owner, name, Arguments.requireSafePath(sha, SHA));
    }

    /**
     * Lists the notifications of the authenticated user for this repository, unread ones only. See
     * {@link #notifications(GitHubListRepoNotificationsForAuthenticatedUserFilter)}.
     *
     * @return the notifications
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Notification> notifications() throws IOException {
        return notifications(GitHubListRepoNotificationsForAuthenticatedUserFilter.empty());
    }

    /**
     * Lists the notifications of the authenticated user for this repository ({@code GET
     * /repos/{owner}/{repo}/notifications}). GitHub does not support fine-grained tokens for notifications, so the
     * client needs a classic token. Pages are fetched only as the stream is consumed; transport failures during
     * iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the filter for read state, participation and dates
     * @return the notifications
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Notification> notifications(final GitHubListRepoNotificationsForAuthenticatedUserFilter filter)
            throws IOException {
        requireNonNull(filter, FILTER);
        return sdk
                .github()
                .activity()
                .notifications()
                .listRepoNotificationsForAuthenticatedUser(owner, name, filter, GitHubPageRequest.byPage())
                .stream()
                .map(listed -> new Notification(sdk, listed));
    }

    /**
     * Marks all notifications of this repository as read ({@code PUT /repos/{owner}/{repo}/notifications}). GitHub may
     * do this asynchronously.
     *
     * @throws IOException if the transport fails
     */
    public void markNotificationsRead() throws IOException {
        sdk.github().activity().notifications().markRepoNotificationsAsRead(owner, name);
    }

    /**
     * Lists the deploy keys ({@code GET /repos/{owner}/{repo}/keys}). Pages are fetched only as the stream is consumed;
     * transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the deploy keys
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<DeployKey> deployKeys() throws IOException {
        return sdk.github().deployKeys().listDeployKeys(owner, name, GitHubPageRequest.byPage()).stream()
                .map(listed -> new DeployKey(this, listed));
    }

    /**
     * Loads one deploy key ({@code GET /repos/{owner}/{repo}/keys/{key_id}}).
     *
     * @param id the key id
     * @return the key, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<DeployKey> deployKey(final long id) throws IOException {
        return sdk.github().deployKeys().getDeployKey(owner, name, id).map(loaded -> new DeployKey(this, loaded));
    }

    /**
     * Adds a read-only deploy key. See {@link #addDeployKey(String, String, boolean)}.
     *
     * @param title the title of the key
     * @param key the public key in OpenSSH format
     * @return the new key
     * @throws NullPointerException if {@code title} or {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code title} or {@code key} is blank
     * @throws IOException if the transport fails
     */
    public DeployKey addDeployKey(final String title, final String key) throws IOException {
        return addDeployKey(title, key, true);
    }

    /**
     * Adds a deploy key ({@code POST /repos/{owner}/{repo}/keys}).
     *
     * @param title the title of the key
     * @param key the public key in OpenSSH format
     * @param isReadOnly {@code true} for read access only, {@code false} to allow pushing
     * @return the new key
     * @throws NullPointerException if {@code title} or {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code title} or {@code key} is blank
     * @throws IOException if the transport fails
     */
    public DeployKey addDeployKey(final String title, final String key, final boolean isReadOnly) throws IOException {
        return new DeployKey(
                this,
                sdk.github()
                        .deployKeys()
                        .createDeployKey(
                                owner,
                                name,
                                new GitHubCreateDeployKeyPayload(
                                        requireNotBlank(title, TITLE), requireNotBlank(key, "key"), isReadOnly)));
    }

    /**
     * Lists the deployments ({@code GET /repos/{owner}/{repo}/deployments}). See
     * {@link #deployments(GitHubListDeploymentsFilter)}.
     *
     * @return the deployments, newest first
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Deployment> deployments() throws IOException {
        return deployments(GitHubListDeploymentsFilter.empty());
    }

    /**
     * Lists the deployments ({@code GET /repos/{owner}/{repo}/deployments}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the filter for sha, ref, task and environment
     * @return the deployments, newest first
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Deployment> deployments(final GitHubListDeploymentsFilter filter) throws IOException {
        return sdk
                .github()
                .deployments()
                .deployment()
                .listDeployments(owner, name, requireNonNull(filter, FILTER), GitHubPageRequest.byPage())
                .stream()
                .map(listed -> new Deployment(this, listed));
    }

    /**
     * Loads one deployment ({@code GET /repos/{owner}/{repo}/deployments/{deployment_id}}).
     *
     * @param id the deployment id
     * @return the deployment, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Deployment> deployment(final long id) throws IOException {
        return sdk.github()
                .deployments()
                .deployment()
                .getDeployment(owner, name, id)
                .map(loaded -> new Deployment(this, loaded));
    }

    /**
     * Creates a deployment of a ref to an environment. GitHub's {@code auto_merge} is switched off, so GitHub never
     * merges the default branch into the ref; the required status checks stay at GitHub's default, which is that all of
     * them must pass. See {@link #deploy(GitHubCreateDeploymentPayload)} for every option.
     *
     * @param ref the branch, tag or sha to deploy
     * @param environment the name of the target environment
     * @return the new deployment
     * @throws NullPointerException if {@code ref} or {@code environment} is {@code null}
     * @throws IllegalArgumentException if {@code ref} or {@code environment} is blank
     * @throws IllegalStateException if GitHub answers with a merge instead of a deployment
     * @throws IOException if the transport fails
     */
    public Deployment deploy(final String ref, final String environment) throws IOException {
        return deploy(new GitHubCreateDeploymentPayload(
                Arguments.requireSafePath(ref, REF),
                null,
                false,
                null,
                null,
                requireNotBlank(environment, "environment"),
                null,
                null,
                null));
    }

    /**
     * Creates a deployment with every option the client offers ({@code POST /repos/{owner}/{repo}/deployments}).
     *
     * @param payload the ref, environment and options
     * @return the new deployment
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IllegalStateException if GitHub answers with a merge of the default branch into the ref instead of a
     *     deployment, which happens only when {@code auto_merge} is on; the message is GitHub's
     * @throws IOException if the transport fails
     */
    public Deployment deploy(final GitHubCreateDeploymentPayload payload) throws IOException {
        final GitHubCreateDeploymentResult result =
                sdk.github().deployments().deployment().createDeployment(owner, name, requireNonNull(payload, PAYLOAD));
        return switch (result) {
            case GitHubCreateDeploymentResult.Created(final var deployment) -> new Deployment(this, deployment);
            case GitHubCreateDeploymentResult.MergedBranch(final var merged) ->
                throw new IllegalStateException(merged.message());
        };
    }

    /**
     * Lists the environments ({@code GET /repos/{owner}/{repo}/environments}). Pages are fetched only as the stream is
     * consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the environments
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<Environment> environments() throws IOException {
        return sdk
                .github()
                .deployments()
                .environments()
                .getAllEnvironments(owner, name, GitHubPageRequest.byPage())
                .stream()
                .map(listed -> new Environment(this, listed));
    }

    /**
     * Loads one environment ({@code GET /repos/{owner}/{repo}/environments/{environment_name}}).
     *
     * @param environmentName the environment name
     * @return the environment, or empty when GitHub answers HTTP 404
     * @throws NullPointerException if {@code environmentName} is {@code null}
     * @throws IllegalArgumentException if {@code environmentName} is blank
     * @throws IOException if the transport fails
     */
    public Optional<Environment> environment(final String environmentName) throws IOException {
        return sdk.github()
                .deployments()
                .environments()
                .getEnvironment(owner, name, requireNotBlank(environmentName, ENVIRONMENT_NAME))
                .map(loaded -> new Environment(this, loaded));
    }

    /**
     * Creates an environment without protection rules, or returns the existing one unchanged: it loads the environment
     * first and sends nothing when it exists, because the PUT of the endpoint can reset the settings it does not carry.
     * See {@link #saveEnvironment(String, GitHubCreateOrUpdateEnvironmentPayload)}.
     *
     * @param environmentName the environment name
     * @return the environment as GitHub holds it afterwards
     * @throws NullPointerException if {@code environmentName} is {@code null}
     * @throws IllegalArgumentException if {@code environmentName} is blank
     * @throws IOException if the transport fails
     */
    public Environment saveEnvironment(final String environmentName) throws IOException {
        final Optional<Environment> existing = environment(environmentName);
        if (existing.isPresent()) {
            return existing.get();
        }
        return saveEnvironment(environmentName, new GitHubCreateOrUpdateEnvironmentPayload(null, null, null, null));
    }

    /**
     * Creates or updates an environment ({@code PUT /repos/{owner}/{repo}/environments/{environment_name}}). This is a
     * full PUT: a setting the payload leaves {@code null} (wait timer, reviewers, branch policy) can be reset, so load
     * the environment and send again the settings you want to keep.
     *
     * @param environmentName the environment name
     * @param payload the wait timer, reviewers and branch policy
     * @return the environment as GitHub holds it afterwards
     * @throws NullPointerException if {@code environmentName} or {@code payload} is {@code null}
     * @throws IllegalArgumentException if {@code environmentName} is blank
     * @throws IOException if the transport fails
     */
    public Environment saveEnvironment(
            final String environmentName, final GitHubCreateOrUpdateEnvironmentPayload payload) throws IOException {
        return new Environment(
                this,
                sdk.github()
                        .deployments()
                        .environments()
                        .createOrUpdateEnvironment(
                                owner,
                                name,
                                requireNotBlank(environmentName, ENVIRONMENT_NAME),
                                requireNonNull(payload, PAYLOAD)));
    }

    /**
     * Loads the GitHub Pages site ({@code GET /repos/{owner}/{repo}/pages}).
     *
     * @return the site, or empty when the repository has no Pages site or GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<PagesSite> pages() throws IOException {
        return sdk.github().pages().getPages(owner, name).map(loaded -> new PagesSite(this, loaded));
    }

    /**
     * Turns on GitHub Pages, built from the root of a branch. See {@link #enablePages(GitHubCreatePagesSitePayload)}
     * for other sources.
     *
     * @param branch the branch to build from
     * @return the new site
     * @throws NullPointerException if {@code branch} is {@code null}
     * @throws IllegalArgumentException if {@code branch} is blank, has a control character or has a {@code .} or
     *     {@code ..} segment
     * @throws IOException if the transport fails
     */
    public PagesSite enablePages(final String branch) throws IOException {
        return enablePages(new GitHubCreatePagesSitePayload(
                null,
                new GitHubCreatePagesSiteSource(
                        Arguments.requireSafePath(branch, BRANCH), GitHubPagesSourcePath.ROOT)));
    }

    /**
     * Turns on GitHub Pages ({@code POST /repos/{owner}/{repo}/pages}). A private repository needs a plan that includes
     * Pages for private repositories, otherwise GitHub answers HTTP 403.
     *
     * @param payload the build type and source
     * @return the new site
     * @throws NullPointerException if {@code payload} is {@code null}
     * @throws IOException if the transport fails
     */
    public PagesSite enablePages(final GitHubCreatePagesSitePayload payload) throws IOException {
        return new PagesSite(this, sdk.github().pages().createPagesSite(owner, name, requireNonNull(payload, PAYLOAD)));
    }

    /**
     * Lists the Dependabot alerts ({@code GET /repos/{owner}/{repo}/dependabot/alerts}). See
     * {@link #dependabotAlerts(GitHubListAlertsForRepoFilter)}.
     *
     * @return the alerts
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<DependabotAlert> dependabotAlerts() throws IOException {
        return dependabotAlerts(GitHubListAlertsForRepoFilter.empty());
    }

    /**
     * Lists the Dependabot alerts ({@code GET /repos/{owner}/{repo}/dependabot/alerts}). Pages are fetched only as the
     * stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the filter for state, severity, ecosystem, package and more
     * @return the alerts
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<DependabotAlert> dependabotAlerts(final GitHubListAlertsForRepoFilter filter) throws IOException {
        return sdk
                .github()
                .dependabot()
                .alerts()
                .listAlertsForRepo(owner, name, requireNonNull(filter, FILTER))
                .stream()
                .map(listed -> new DependabotAlert(this, listed));
    }

    /**
     * Loads one Dependabot alert ({@code GET /repos/{owner}/{repo}/dependabot/alerts/{alert_number}}).
     *
     * @param number the alert number
     * @return the alert, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<DependabotAlert> dependabotAlert(final int number) throws IOException {
        return sdk.github()
                .dependabot()
                .alerts()
                .getAlert(owner, name, number)
                .map(loaded -> new DependabotAlert(this, loaded));
    }

    /**
     * Lists the secret scanning alerts ({@code GET /repos/{owner}/{repo}/secret-scanning/alerts}). See
     * {@link #secretScanningAlerts(GitHubListSecretScanningAlertsFilter)}.
     *
     * @return the alerts
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<SecretScanningAlert> secretScanningAlerts() throws IOException {
        return secretScanningAlerts(GitHubListSecretScanningAlertsFilter.empty());
    }

    /**
     * Lists the secret scanning alerts ({@code GET /repos/{owner}/{repo}/secret-scanning/alerts}). GitHub's list holds
     * less than a single lookup, so each alert is loaded with one more request as the stream reaches it;
     * {@code limit(...)} bounds the cost. Pages are fetched only as the stream is consumed; transport failures during
     * iteration are {@link java.io.UncheckedIOException}.
     *
     * @param filter the filter for state, secret type, provider and more
     * @return the alerts
     * @throws NullPointerException if {@code filter} is {@code null}
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<SecretScanningAlert> secretScanningAlerts(final GitHubListSecretScanningAlertsFilter filter)
            throws IOException {
        return sdk
                .github()
                .secretScanning()
                .alerts()
                .listRepoAlerts(owner, name, requireNonNull(filter, FILTER))
                .stream()
                .map(this::loadSecretScanningAlert)
                .flatMap(Optional::stream);
    }

    /**
     * Loads one secret scanning alert ({@code GET /repos/{owner}/{repo}/secret-scanning/alerts/{alert_number}}).
     *
     * @param number the alert number
     * @return the alert, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<SecretScanningAlert> secretScanningAlert(final int number) throws IOException {
        return sdk.github()
                .secretScanning()
                .alerts()
                .getAlert(owner, name, number)
                .map(loaded -> new SecretScanningAlert(this, loaded));
    }

    /**
     * Lists the code scanning alerts ({@code GET /repos/{owner}/{repo}/code-scanning/alerts}). GitHub's list holds less
     * than a single lookup, so each alert is loaded with one more request as the stream reaches it; {@code limit(...)}
     * bounds the cost. Pages are fetched only as the stream is consumed; transport failures during iteration are
     * {@link java.io.UncheckedIOException}.
     *
     * @return the alerts
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<CodeScanningAlert> codeScanningAlerts() throws IOException {
        return sdk.github().codeScanning().codeScanning().listAlertsForRepo(owner, name).stream()
                .map(this::loadCodeScanningAlert)
                .flatMap(Optional::stream);
    }

    /**
     * Loads one code scanning alert ({@code GET /repos/{owner}/{repo}/code-scanning/alerts/{alert_number}}).
     *
     * @param number the alert number
     * @return the alert, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<CodeScanningAlert> codeScanningAlert(final int number) throws IOException {
        return sdk.github()
                .codeScanning()
                .codeScanning()
                .getAlert(owner, name, number)
                .map(loaded -> new CodeScanningAlert(this, loaded));
    }

    /**
     * Returns the temporary interaction limit of the repository, which restricts who may comment and open issues and
     * pull requests for a while. Nothing is requested until a method of the result is called.
     *
     * @return the interaction limit
     */
    public RepositoryInteractions interactions() {
        return new RepositoryInteractions(this);
    }

    /**
     * Returns the code quality findings and setup of the repository. Nothing is requested until a method of the result
     * is called.
     *
     * @return the code quality handle
     */
    public RepositoryCodeQuality codeQuality() {
        return new RepositoryCodeQuality(this);
    }

    /**
     * Returns the security advisories of the repository, the private reports a maintainer prepares before a
     * vulnerability is published. Nothing is requested until a method of the result is called.
     *
     * @return the advisories
     */
    public RepositoryAdvisories advisories() {
        return new RepositoryAdvisories(this);
    }

    /**
     * Returns the dependency graph of the repository: the software bill of materials, dependency changes between refs
     * and dependency submission. Nothing is requested until a method of the result is called.
     *
     * @return the dependency graph
     */
    public RepositoryDependencies dependencies() {
        return new RepositoryDependencies(this);
    }

    /**
     * Returns the GitHub Actions caches of the repository with their usage and limits. Nothing is requested until a
     * method of the result is called.
     *
     * @return the Actions caches
     */
    public RepositoryActionsCache actionsCache() {
        return new RepositoryActionsCache(this);
    }

    /**
     * Returns the code scanning analyses, SARIF uploads, CodeQL databases and default setup of the repository. The
     * alerts are on {@link #codeScanningAlerts()}. Nothing is requested until a method of the result is called.
     *
     * @return the code scanning handle
     */
    public RepositoryCodeScanning codeScanning() {
        return new RepositoryCodeScanning(this);
    }

    /**
     * Returns the self-hosted runners of the repository. Nothing is requested until a method of the result is called.
     *
     * @return the runners
     */
    public SelfHostedRunners selfHostedRunners() {
        return SelfHostedRunners.forRepository(this);
    }

    /**
     * Loads the OpenID Connect subject claim customization of the repository ({@code GET
     * /repos/{owner}/{repo}/actions/oidc/customization/sub}) as GitHub returns it.
     *
     * @return the customization
     * @throws IOException if the transport fails
     */
    public GitHubOidcCustomSubRepo oidcSubjectClaim() throws IOException {
        return sdk.github().actions().oidc().getCustomOidcSubClaimForRepo(owner, name);
    }

    /**
     * Sets the OpenID Connect subject claim customization of the repository ({@code PUT
     * /repos/{owner}/{repo}/actions/oidc/customization/sub}). It needs the Administration permission.
     *
     * @param isDefaultUsed {@code true} to use the organization default, which ignores the claim keys
     * @param includeClaimKeys the claim keys that make up the subject when the default is not used
     * @param isImmutableSubjectUsed {@code true} to use the immutable subject, or {@code null} to leave it unset
     * @throws NullPointerException if {@code includeClaimKeys} or a key is {@code null}
     * @throws IllegalArgumentException if a key is blank
     * @throws IOException if the transport fails
     */
    public void setOidcSubjectClaim(
            final boolean isDefaultUsed,
            final List<String> includeClaimKeys,
            final @Nullable Boolean isImmutableSubjectUsed)
            throws IOException {
        sdk.github()
                .actions()
                .oidc()
                .setCustomOidcSubClaimForRepo(
                        owner,
                        name,
                        new GitHubSetCustomOidcSubClaimForRepoPayload(
                                isDefaultUsed,
                                Arguments.requireNotBlankElements(includeClaimKeys, "includeClaimKeys"),
                                isImmutableSubjectUsed));
    }

    /**
     * Returns the security switches of the repository, such as Dependabot alerts and private vulnerability reporting.
     * Nothing is requested until a method of the result is called.
     *
     * @return the security switches
     */
    public RepositorySecurity security() {
        return new RepositorySecurity(this);
    }

    /**
     * Returns the traffic of the repository, which GitHub shows only to people with push access. Nothing is requested
     * until a method of the result is called.
     *
     * @return the traffic
     */
    public RepositoryTraffic traffic() {
        return new RepositoryTraffic(this);
    }

    /**
     * Returns the statistics of the repository, such as commit activity. Nothing is requested until a method of the
     * result is called.
     *
     * @return the statistics
     */
    public RepositoryStatistics statistics() {
        return new RepositoryStatistics(this);
    }

    /**
     * Loads the community profile ({@code GET /repos/{owner}/{repo}/community/profile}), which says which community
     * files, such as the code of conduct and the contributing guide, the repository has.
     *
     * @return the profile as GitHub returns it
     * @throws IOException if the transport fails
     */
    public GitHubCommunityProfile communityProfile() throws IOException {
        return sdk.github().metrics().community().getCommunityProfileMetrics(owner, name);
    }

    /**
     * Lists the comments on all commits ({@code GET /repos/{owner}/{repo}/comments}). Pages are fetched only as the
     * stream is consumed; transport failures during iteration are {@link java.io.UncheckedIOException}.
     *
     * @return the commit comments
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<CommitComment> commitComments() throws IOException {
        return sdk
                .github()
                .commits()
                .comments()
                .listCommitCommentsForRepo(owner, name, GitHubPageRequest.byPage())
                .stream()
                .map(listed -> new CommitComment(this, listed));
    }

    /**
     * Loads one commit comment ({@code GET /repos/{owner}/{repo}/comments/{comment_id}}).
     *
     * @param id the comment id
     * @return the comment, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<CommitComment> commitComment(final long id) throws IOException {
        return sdk.github()
                .commits()
                .comments()
                .getCommitComment(owner, name, id)
                .map(loaded -> new CommitComment(this, loaded));
    }

    /**
     * Lists the autolink references ({@code GET /repos/{owner}/{repo}/autolinks}).
     *
     * @return the autolinks
     * @throws IOException if the transport fails
     */
    public Stream<Autolink> autolinks() throws IOException {
        return sdk.github().repositories().autolinks().listAutolinks(owner, name).stream()
                .map(listed -> new Autolink(this, listed));
    }

    /**
     * Loads one autolink reference ({@code GET /repos/{owner}/{repo}/autolinks/{autolink_id}}).
     *
     * @param id the autolink id
     * @return the autolink, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Autolink> autolink(final long id) throws IOException {
        return sdk.github()
                .repositories()
                .autolinks()
                .getAutolink(owner, name, id)
                .map(loaded -> new Autolink(this, loaded));
    }

    /**
     * Adds an autolink reference that turns text such as {@code TICKET-123} into a link ({@code POST
     * /repos/{owner}/{repo}/autolinks}).
     *
     * @param keyPrefix the text that starts a reference, for example {@code TICKET-}
     * @param urlTemplate the link the reference turns into, with {@code <num>} where the number goes
     * @return the new autolink
     * @throws NullPointerException if {@code keyPrefix} or {@code urlTemplate} is {@code null}
     * @throws IllegalArgumentException if {@code keyPrefix} or {@code urlTemplate} is blank
     * @throws IOException if the transport fails
     */
    public Autolink addAutolink(final String keyPrefix, final String urlTemplate) throws IOException {
        return new Autolink(
                this,
                sdk.github()
                        .repositories()
                        .autolinks()
                        .createAutolink(
                                owner,
                                name,
                                new GitHubCreateAutolinkPayload(
                                        requireNotBlank(keyPrefix, "keyPrefix"),
                                        requireNotBlank(urlTemplate, "urlTemplate"),
                                        null)));
    }

    /**
     * Lists the rulesets that apply to the repository, including those inherited from the organization ({@code GET
     * /repos/{owner}/{repo}/rulesets}). GitHub lists them without their rules, so each ruleset is loaded again as the
     * stream reaches it; the stream is lazy and can be used once.
     *
     * @return the rulesets
     * @throws IOException if the transport fails
     * @throws java.io.UncheckedIOException while consuming the stream, if loading a ruleset fails
     */
    public Stream<Ruleset> rulesets() throws IOException {
        return sdk.github().repositories().rules().getRepoRulesets(owner, name).stream()
                .map(listed -> Lookups.find(() -> ruleset(listed.id())))
                .flatMap(Optional::stream);
    }

    /**
     * Loads one ruleset ({@code GET /repos/{owner}/{repo}/rulesets/{ruleset_id}}).
     *
     * @param id the ruleset id
     * @return the ruleset, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<Ruleset> ruleset(final long id) throws IOException {
        return sdk.github()
                .repositories()
                .rules()
                .getRepoRuleset(owner, name, id)
                .map(loaded -> new Ruleset(this, loaded));
    }

    /**
     * Starts a new ruleset; nothing is sent until {@link RulesetDraft#create()}.
     *
     * @param rulesetName the name of the ruleset
     * @param target what the ruleset applies to
     * @param enforcement whether the ruleset is enforced, only evaluated, or disabled
     * @return a new draft
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code rulesetName} is blank
     */
    public RulesetDraft newRuleset(
            final String rulesetName, final RulesetTarget target, final RulesetEnforcement enforcement) {
        return new RulesetDraft(this, rulesetName, target, enforcement);
    }

    /**
     * Lists the rule suites, which record how the rulesets judged each push and merge, newest first ({@code GET
     * /repos/{owner}/{repo}/rulesets/rule-suites}). The stream is lazy and can be used once.
     *
     * @return the rule suites as GitHub returns them
     * @throws IOException if the transport fails
     */
    public Stream<GitHubRuleSuiteListItem> ruleSuites() throws IOException {
        return sdk.github().repositories().ruleSuites().getRepoRuleSuites(owner, name).stream();
    }

    /**
     * Loads one rule suite with the evaluation of each rule ({@code GET
     * /repos/{owner}/{repo}/rulesets/rule-suites/{rule_suite_id}}).
     *
     * @param id the rule suite id
     * @return the rule suite as GitHub returns it, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<GitHubRuleSuite> ruleSuite(final long id) throws IOException {
        return sdk.github().repositories().ruleSuites().getRepoRuleSuite(owner, name, id);
    }

    /**
     * Lists the events of the repository, newest first ({@code GET /repos/{owner}/{repo}/events}). GitHub keeps only
     * the last 90 days of events, at most 300, and caches the feed for a few seconds, so a new event can take a moment
     * to show. The stream is lazy and can be used once.
     *
     * @return the events as GitHub returns them
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubEvent> events() throws IOException {
        return sdk.github().activity().events().listRepoEvents(owner, name).stream();
    }

    /**
     * Lists the public events of the repository network, which is the repository and its forks ({@code GET
     * /networks/{owner}/{repo}/events}). GitHub keeps only the last 90 days of events, at most 300, and caches the feed
     * for a few seconds, so a new event can take a moment to show. The stream is lazy and can be used once.
     *
     * @return the events as GitHub returns them
     * @throws IOException if the first page cannot be loaded
     */
    public Stream<GitHubEvent> networkEvents() throws IOException {
        return sdk.github().activity().events().listPublicEventsForRepoNetwork(owner, name).stream();
    }

    /**
     * Loads one check suite ({@code GET /repos/{owner}/{repo}/check-suites/{check_suite_id}}).
     *
     * @param id the check suite id
     * @return the suite, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<CheckSuite> checkSuite(final long id) throws IOException {
        return sdk.github().checks().suites().getSuite(owner, name, id).map(loaded -> new CheckSuite(this, loaded));
    }

    /**
     * Loads the license of the repository ({@code GET /repos/{owner}/{repo}/license}) as GitHub returns it.
     *
     * @return the license file with the detected license, or empty when GitHub answers HTTP 404
     * @throws IOException if the transport fails
     */
    public Optional<GitHubLicenseContent> license() throws IOException {
        return sdk.github().licenses().getForRepo(owner, name);
    }

    Repository apply(final GitHubUpdatePayload payload) throws IOException {
        final GitHubFullRepository updated =
                sdk.github().repositories().repository().update(owner, name, payload);
        return new Repository(sdk, owner, updated.name(), updated);
    }

    private Repository transferTo(final String newOwner, final @Nullable String newName) throws IOException {
        final GitHubMinimalRepository moved = sdk.github()
                .repositories()
                .repository()
                .transfer(owner, name, new GitHubTransferPayload(requireNotBlank(newOwner, "newOwner"), newName, null));
        return sdk.repository(moved.owner().login(), moved.name());
    }

    private void dispatch(final String eventType, final @Nullable Map<String, Object> clientPayload)
            throws IOException {
        sdk.github()
                .repositories()
                .repository()
                .createDispatchEvent(
                        owner,
                        name,
                        new GitHubCreateDispatchEventPayload(requireNotBlank(eventType, "eventType"), clientPayload));
    }

    private Optional<PullRequest> loadPullRequest(final GitHubPullRequestSimple listed) {
        final int number = requireNonNull(listed.number(), "number");
        return Lookups.find(() -> pullRequest(number));
    }

    private Optional<SecretScanningAlert> loadSecretScanningAlert(final GitHubSecretScanningAlert listed) {
        final int number = requireNonNull(listed.number(), "number");
        return Lookups.find(() -> secretScanningAlert(number));
    }

    private Optional<CodeScanningAlert> loadCodeScanningAlert(final GitHubCodeScanningAlertItems listed) {
        final int number = requireNonNull(listed.number(), "number");
        return Lookups.find(() -> codeScanningAlert(number));
    }

    private Optional<Branch> loadBranch(final GitHubShortBranch listed) {
        return Lookups.find(() -> branch(listed.name()));
    }

    private Repository createFork(final @Nullable GitHubCreateForkPayload payload) throws IOException {
        final GitHubFullRepository created = sdk.github().repositories().forks().createFork(owner, name, payload);
        return new Repository(sdk, created.owner().login(), created.name(), created);
    }

    private Optional<RepositoryFile> read(final String path, final @Nullable String ref) throws IOException {
        return sdk.github()
                .repositories()
                .contents()
                .getContent(owner, name, path, new GitHubGetContentFilter(ref))
                .flatMap(result -> switch (result) {
                    case GitHubContentFile file -> Optional.of(new RepositoryFile(this, file, ref));
                    case GitHubContentDirectory ignored -> Optional.empty();
                    case GitHubContentSymlink ignored -> Optional.empty();
                    case GitHubContentSubmodule ignored -> Optional.empty();
                });
    }

    private RepositoryFile write(
            final String path, final String message, final byte[] content, final @Nullable String branch)
            throws IOException {
        Arguments.requireSafePath(path, "path");
        requireNotBlank(message, "message");
        requireNonNull(content, "content");
        final String sha = read(path, branch).map(RepositoryFile::sha).orElse(null);
        sdk.github()
                .repositories()
                .contents()
                .createOrUpdateFileContents(
                        owner,
                        name,
                        path,
                        new GitHubCreateOrUpdateFileContentsPayload(
                                message, Base64.getEncoder().encodeToString(content), sha, branch, null, null));
        return read(path, branch)
                .orElseThrow(() -> new IllegalStateException("File " + path + " is gone after the write"));
    }
}
