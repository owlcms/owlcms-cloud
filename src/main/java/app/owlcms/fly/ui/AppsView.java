package app.owlcms.fly.ui;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletRequest;

import app.owlcms.fly.commands.CreationErrorException;
import app.owlcms.fly.commands.FlyCtlCommands;
import app.owlcms.fly.commands.NameTakenException;
import app.owlcms.fly.flydata.App;
import app.owlcms.fly.flydata.AppType;
import app.owlcms.fly.flydata.EarthLocation;
import app.owlcms.fly.flydata.GeoLocator;
import app.owlcms.fly.flydata.VersionInfo;
import jakarta.servlet.http.HttpServletRequest;

/**
 * The main view contains a text field for getting the user name and a button
 * that shows a greeting message in a
 * notification.
 */
@Route("apps")
public class AppsView extends VerticalLayout {

	private static final String LEFT_LABEL_WIDTH = "14em";
	private long lastClick = 0;
	@SuppressWarnings("unused")
	final private Logger logger = LoggerFactory.getLogger(AppsView.class);
	private LogDialog logDialog;
	private FlyCtlCommands flyCommands;
	private VerticalLayout intro;
	private VerticalLayout apps;
	private String regionCode;
	private String clientIpString;
	
	// Cache latest versions to avoid multiple fetches
	private String cachedOwlcmsVersion = null;
	private String cachedTrackerVersion = null;

	public AppsView() {
		clientIpString = getClientIp();
		logDialog = new LogDialog();
		flyCommands = new FlyCtlCommands(UI.getCurrent(), logDialog);
		if (flyCommands.getToken() == null) {
			Login loginOverlay = new Login(flyCommands);
			loginOverlay.setCallback(() -> {
				getServerLocations();
				loginOverlay.setOpened(false);
				this.removeAll();
				showApplicationView();
			});
			loginOverlay.setOpened(true);
			add(loginOverlay);
		} else {
			getServerLocations();
			showApplicationView();
		}
	}

	private void getServerLocations() {
		EarthLocation clientIpLocation;
		clientIpLocation = GeoLocator.locate(clientIpString);
		// list is sorted with closest region at the top
		serverLocations = flyCommands.getServerLocations(clientIpLocation);
	}

	private void showApplicationView() {
		this.setSpacing(false);
		this.setHeightFull();
		H2 title = new H2("owlcms Cloud Applications - " + flyCommands.getUserName());
		Button logoutButton = new Button("Logout", (e) -> {
			flyCommands.setToken(null);
			Notification.show("Logged out", 1000, Position.TOP_END);
			UI.getCurrent().navigate("");
		});

		intro = buildIntro();
		apps = buildAppsPlaceholder();
		add(new HorizontalLayout(title, logoutButton), intro, apps);
		UI ui = UI.getCurrent();
		doListApplications(apps, ui);
	}

	private VerticalLayout buildAppsPlaceholder() {
		VerticalLayout apps = new VerticalLayout();
		apps.setMargin(false);
		apps.setPadding(false);
		return apps;
	}

	private ConfirmDialog buildDeletionDialog(App app, Runnable callback) {
		ConfirmDialog deletionDialog = new ConfirmDialog();
		deletionDialog.setHeader("Deletion Confirmation Required");
		if (app.appType == AppType.OWLCMS) {
			deletionDialog.setText(new Html(
					"""
							<div>
							   This will remove the application and make the name available again.
							   <br />
							   NOTE: the database will also be deleted; make sure you have
							   exported the database if you need to keep the information.
							</div>
							"""));
		} else {
			deletionDialog.setText(new Html(
					"""
							<div>
							   This will remove the application and make the name available again.
							</div>
							"""));
		}

		deletionDialog.setConfirmText("Delete");
		deletionDialog.setConfirmButtonTheme("error primary");
		deletionDialog.setCancelButtonTheme("primary");
		deletionDialog.setCancelable(true);
		deletionDialog.setCancelText("Cancel");
		deletionDialog.addConfirmListener(e -> {

			if (app.appType == AppType.OWLCMS) {
				Map<AppType, App> apps2 = flyCommands.getApps();
				App dbApp = apps2.get(AppType.DB);
				if (dbApp != null) {
				logDialog.append("Deleting OWLCMS " + app.name, UI.getCurrent());
				flyCommands.appDestroy(app, null);
				logDialog.append("Deleting OWLCMS database " + dbApp.name, UI.getCurrent());
				flyCommands.appDestroy(dbApp, callback);
			} else {
				logDialog.append("Deleting OWLCMS - no database " + app.name, UI.getCurrent());
					flyCommands.appDestroy(app, callback);
				}
			} else {
				logDialog.append("Deleting PUBLICRESULTS " + app.name, UI.getCurrent());
				flyCommands.appDestroy(app, callback);
			}
		});
		deletionDialog.addCancelListener(e -> {
			deletionDialog.close();
		});
		return deletionDialog;
	}

	private ConfirmDialog buildStopDialog(App app, Runnable callback) {
		ConfirmDialog stopDialog = new ConfirmDialog();
		stopDialog.setHeader("Stop Confirmation Required");
		if (app.appType == AppType.OWLCMS) {
			stopDialog.setText(new Html(
					"""
							<div>
							   This will stop the application and minimize further billing.
							   <br />
							   You will only be billed a very small amount for disk space.
							   NOTE: the database will also be stopped, but not deleted.
							   <br />
							   To completely stop billing, export your database and delete the application.
							</div>
							"""));
		} else {
			stopDialog.setText(new Html(
					"""
							<div>
							   This will stop the application and stop further billing.
							   <br />
							   You will only be billed a very small amount for disk space.
							   <br />
							   To completely stop billing, delete the application.
							</div>
							"""));
		}

		stopDialog.setConfirmText("Stop");
		stopDialog.setConfirmButtonTheme("error primary");
		stopDialog.setCancelButtonTheme("primary");
		stopDialog.setCancelable(true);
		stopDialog.setCancelText("Cancel");
		stopDialog.addConfirmListener(e -> {

			if (app.appType == AppType.OWLCMS) {
				Map<AppType, App> apps2 = flyCommands.getApps();
				App dbApp = apps2.get(AppType.DB);
				if (dbApp != null) {
				logDialog.append("Suspending OWLCMS " + app.name, UI.getCurrent());
				flyCommands.appStop(app, null);
				logDialog.append("Suspending OWLCMS database " + dbApp.name, UI.getCurrent());
				flyCommands.appStop(dbApp, callback);
			} else {
				logDialog.append("Suspending OWLCMS - no database " + app.name, UI.getCurrent());
					flyCommands.appStop(app, callback);
				}
			} else if (app.appType == AppType.TRACKER) {
				logDialog.append("Suspending TRACKER " + app.name, UI.getCurrent());
				flyCommands.appStop(app, callback);
			} else {
				logDialog.append("Suspending PUBLICRESULTS " + app.name, UI.getCurrent());
				flyCommands.appStop(app, callback);
			}
		});
		stopDialog.addCancelListener(e -> {
			stopDialog.close();
		});
		return stopDialog;
	}

	private VerticalLayout buildIntro() {
		Html p1 = new Html(
				"""
						<div style="width: 60em">
						This page creates and manages your owlcms applications on the fly.io cloud.
						</div>
						""");
		VerticalLayout intro = new VerticalLayout(p1);
		intro.setSpacing(false);
		intro.setPadding(false);
		intro.setMargin(false);
		intro.getStyle().set("margin-top", "1em");
		return intro;
	}

	public String getClientIp() {
		HttpServletRequest request;
		VaadinServletRequest current = VaadinServletRequest.getCurrent();
		request = current.getHttpServletRequest();
		String remoteAddr = "";
		if (request != null) {
			remoteAddr = request.getHeader("X-FORWARDED-FOR");
			if (remoteAddr == null || "".equals(remoteAddr)) {
				remoteAddr = request.getRemoteAddr();
			} else if (remoteAddr.contains(", ")) {
				String[] remoteAddresses = remoteAddr.split(", ");
				remoteAddr = remoteAddresses[0];
			}
		}
		return remoteAddr;
	}

	EarthLocation serverLoc = null;
	private List<EarthLocation> serverLocations;

	private void doListApplications(VerticalLayout appsArea, UI ui) {
		long timeMillis = System.currentTimeMillis();
		if (timeMillis - lastClick < 100) {
			lastClick = timeMillis;
			return;
		}
		lastClick = timeMillis;

		appsArea.removeAll();
		logDialog.clear(ui);
		logDialog.show();
		logDialog.append("Retrieving your application configurations. Please wait.", ui);
		ui.push();

		new Thread(() -> {
			// ui.access(() -> {
			// this also retrieves the region for the applications if available
			Map<AppType, App> appsList = flyCommands.getApps();
			regionCode = null;
			for (App app : appsList.values()) {
				if (app.regionCode != null && !app.regionCode.isBlank()) {
					regionCode = app.regionCode;
				}
				break;
			}

			ui.access(() -> {
				showApps(appsList, appsArea);
				logDialog.clear(ui);
				logDialog.hide();
			});
		}).start();
	}

	private void doSilentListRefresh(VerticalLayout appsArea, UI ui) {
		new Thread(() -> {
			Map<AppType, App> appsList = flyCommands.getApps();
			regionCode = null;
			for (App app : appsList.values()) {
				if (app.regionCode != null && !app.regionCode.isBlank()) {
					regionCode = app.regionCode;
				}
				break;
			}

			ui.access(() -> {
				appsArea.removeAll();
				showApps(appsList, appsArea);
			});
		}).start();
	}

	private Div showApplication(App app) {
		HorizontalLayout appSection = new HorizontalLayout();
		appSection.setMargin(false);
		appSection.setPadding(false);
		appSection.setAlignItems(Alignment.START);

		// Left column: label
		NativeLabel label = new NativeLabel(app.appType.toString());
		label.setWidth(LEFT_LABEL_WIDTH);
		appSection.add(label);

		// Right column: all content (explanation, version, controls)
		VerticalLayout contentDiv = new VerticalLayout();
		contentDiv.setMargin(false);
		contentDiv.setPadding(false);
		contentDiv.setSpacing(false);

		// Explanation (add directly, avoid nested VerticalLayout)
		Html explanation = new Html(getExplanationForAppType(app.appType));
		contentDiv.add(explanation);

		UI ui = UI.getCurrent();

		// Details and controls row
		HorizontalLayout controlsLayout = new HorizontalLayout();
		controlsLayout.setMargin(false);
		controlsLayout.setPadding(false);
		controlsLayout.setAlignItems(Alignment.CENTER);

		if (app.created) {
			showExistingApplication(app, controlsLayout, ui);
			contentDiv.add(controlsLayout);
		} else {
			showNewApplication(app, contentDiv, controlsLayout, ui);
		}
		appSection.add(contentDiv);

		Div wrapper = new Div(appSection);
		wrapper.getStyle().set("margin-bottom", "0");
		return wrapper;
	}

	private String getExplanationForAppType(AppType appType) {
		return switch (appType) {
			case OWLCMS ->
				"""
					<ul style="line-height: 1.4; width: 45em; margin: 0; padding-left: 1em;">
						<li>OWLCMS runs the competition and drives the screens used at the site.
						<li><u>You don't need OWLCMS in the cloud if you are running OWLCMS on a laptop at the site</u> and only want remote scoreboards.
					</ul>
				""";
				case TRACKER ->
				"""
					<ul style="line-height: 1.4; width: 45em; margin: 0; padding-left: 1em;">
						<li>When running in the cloud, TRACKER is used to show scoreboards to anyone on the internet.
						<li><u>You don't need TRACKER in the cloud if you don't want remote scoreboards.</u>
						<li><b>Note: TRACKER requires OWLCMS version 64 or later</b></li>
						<li>To use TRACKER, you need to provide websocket connection URLs in OWLCMS</li>
						<ul>
							<li>Go to the Languages and System Settings page in OWLCMS and select the Connections tab.</li>
							<li>If your tracker application is called <i>live-results</i>, the public results format is<br> <a href="">wss://<i>live-results</i>.fly.dev/ws</a></li>
						</ul>
						<li>The Shared Key set at the bottom of this page protects the communications between OWLCMS and TRACKER.
					</ul>
				""";
			case PUBLICRESULTS ->
				"""
					<ul style="line-height: 1.4; width: 45em; margin: 0; padding-left: 1em;">
						<li>PUBLICRESULTS is the legacy way is used to view scoreboards from the internet.
						<li>PUBLICRESULTS is being replaced by TRACKER, which provides more features.</li>
						<li>The Shared Key set at the bottom of this page protects the communications between OWLCMS and PUBLICRESULTS.
					</ul>
				""";
			default -> "";
		};
	}

	private void showNewApplication(App app, VerticalLayout contentDiv, HorizontalLayout hl, UI ui) {
		String latestVersion = app.getReferenceVersion();
		// Version info (use Div to avoid nested VerticalLayout)
		Html versionHtml = new Html(
				"""
					<div>latest available version: %s</div>
				""".formatted(latestVersion));
		Div versionInfo = new Div();
		versionInfo.add(versionHtml);
		versionInfo.getStyle().set("width", "20em");
		versionInfo.getStyle().set("margin-top", "0.1em");
		versionInfo.getStyle().set("margin-bottom", "0.1em");
		contentDiv.add(versionInfo);

		TextField nameField = new TextField("Application Name (without .fly.dev)");
		nameField.setAllowedCharPattern("[A-Za-z0-9-]");
		nameField.setValue(app.name);
		nameField.setPlaceholder("Letters, numbers and hyphens");
		nameField.setWidth("20em");
		hl.add(nameField);
		nameField.setRequired(true);
		nameField.setRequiredIndicatorVisible(true);

		ComboBox<EarthLocation> serverCombo = new ComboBox<>();

		serverCombo.setRenderer(new TextRenderer<>(EarthLocation::getFullName));
		serverCombo.setItemLabelGenerator(EarthLocation::getFullName);
		serverCombo.setLabel("Select a server location");
		serverCombo.setWidth("20em");
		serverCombo.setItems(serverLocations);

		if (regionCode != null) {
			serverLoc = serverLocations.stream().filter(l -> regionCode.contentEquals(l.getCode())).findAny()
					.orElse(null);
		} else {
			serverLoc = serverLocations.get(0);
		}
		serverCombo.setValue(serverLoc);

		Button creationButton = new Button("Create",
				e -> {
					String value = nameField.getValue();
					if (value == null || value.isBlank()) {
						nameField.setErrorMessage("You must provide a value");
						nameField.setInvalid(true);
					} else {
						String siteName = value.toLowerCase() + ".fly.dev";

						try {
							flyCommands.createApp(value.toLowerCase());
							nameField.setInvalid(false);
							app.name = value.toLowerCase();
							app.regionCode = serverCombo.getValue().getCode();
							// If version is unknown, use "latest" for deployment
							if (latestVersion.contains("unknown")) {
								VersionInfo updatedInfo;
								if (app.appType == AppType.TRACKER) {
									updatedInfo = new VersionInfo("latest", "https://api.github.com/repos/jflamy/owlcms-tracker/releases");
								} else {
									updatedInfo = new VersionInfo("latest");
								}
								app.setVersionInfo(updatedInfo);
							}
						flyCommands.appCreate(app, () -> doSilentListRefresh(apps, ui));
						} catch (NameTakenException e1) {
							nameField.setErrorMessage(siteName + " is already taken.");
							nameField.setInvalid(true);
						} catch (CreationErrorException e1) {
							nameField.setErrorMessage(e1.getMessage());
							nameField.setInvalid(true);
						}
					}
				});
		hl.add(serverCombo, creationButton);
		contentDiv.add(hl);
	}

	private void showExistingApplication(App app, HorizontalLayout hl, UI ui) {
		// Mark this controls block for existing apps so we can style it via CSS
		hl.addClassName("existingApp");
		hl.getStyle().set("margin-top", "1em");
		Anchor a = new Anchor("https://" + app.name + ".fly.dev", app.name + ".fly.dev", AnchorTarget.BLANK);
		a.getStyle().set("text-decoration", "underline");
		String rawVersion = app.getCurrentVersion();
		String displayVersion = rawVersion + (rawVersion.matches("^[0-9].*$") ? "" : " (version number unknown)");
		String latestVersion = getLatestReleaseVersion(app.appType);
		boolean updateRequired = app.isUpdateRequired();
		VerticalLayout versionInfo = new VerticalLayout(a,
				new Html(
						"""
								<div>your version: %s<br />latest version: %s<span style="color:red">%s</span><br/> region: %s</div>
								"""
								.formatted(
										displayVersion,
										latestVersion,
										updateRequired ? " Please Update" : "",
										app.regionCode)));
		versionInfo.setMargin(false);
		versionInfo.setPadding(false);
		versionInfo.setSpacing(false);
		versionInfo.setWidth("20em");
		hl.add(versionInfo);

		Button updateButton = new Button("Update",
			e -> flyCommands.appDeploy(app, () -> doSilentListRefresh(apps, ui)));
		if (updateRequired) {
			updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		}
		hl.add(updateButton);

		Button restartButton = new Button("Restart",
				e -> {
					flyCommands.appRestart(app);
			});
		hl.add(restartButton);

		ConfirmDialog deletionDialog = buildDeletionDialog(app,
				() -> doListApplications(apps, ui));
		Button deleteButton = new Button("Delete");
		deleteButton.addClickListener(event -> {
			deletionDialog.open();
		});
		hl.add(deleteButton);

		if (!app.stopped) {
			ConfirmDialog stopDialog = buildStopDialog(app,
					() -> {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e1) {
						}
						doListApplications(apps, ui);
					});
			Button stopButton = new Button("Stop");
			stopButton.addClickListener(event -> {
				stopDialog.open();
			});
			hl.add(stopButton);
		}
	}

	private void showSharedKey(VerticalLayout apps) {
		HorizontalLayout sharedKeySection = new HorizontalLayout();
		sharedKeySection.setMargin(false);
		sharedKeySection.setPadding(false);
		sharedKeySection.setAlignItems(Alignment.START);

		// Left column: label
		NativeLabel label = new NativeLabel("Shared Key");
		label.setWidth(LEFT_LABEL_WIDTH);
		sharedKeySection.add(label);

		// Right column: all content (explanation, controls)
		VerticalLayout contentDiv = new VerticalLayout();
		contentDiv.setMargin(false);
		contentDiv.setPadding(false);
		contentDiv.setSpacing(false);

		// Explanation (add directly, avoid nested VerticalLayout)
		Html explanation = new Html(
				"""
						<ul style="line-height: 1.4; width: 45em; margin: 0; padding-left: 1em;">
						<li>Set the shared key to protect communications from OWLCMS to the other applications.
						<li>This is done once initially, and the same key is used for both PUBLICRESULTS and TRACKER.
						<li>You can change it later, but you will need to restart the applications.
						<li><u>If OWLCMS is running on a laptop</u>, copy the key to the Connections configuration in the OWLCMS settings.
						</ul>
						""");
		contentDiv.add(explanation);

		// Controls row
		HorizontalLayout controlsLayout = new HorizontalLayout();
		controlsLayout.setMargin(false);
		controlsLayout.setPadding(false);
		controlsLayout.setAlignItems(Alignment.CENTER);
		controlsLayout.getStyle().set("margin-top", "1em");

		TextField sharedKeyField = new TextField();
		sharedKeyField.setTitle("Shared string between owlcms and public results");
		sharedKeyField.setPlaceholder("enter a shared string");
		sharedKeyField.setWidth("15em");
		sharedKeyField.setValue("");

		Button generateKeyButton = new Button("Generate Shared Key",
				e -> {
					sharedKeyField.setValue(generateRandomString(20));
				});

		Button setSecretsButton = new Button("Set Secrets",
				e -> {
					if (sharedKeyField.getValue() == null || sharedKeyField.getValue().isBlank()) {
						sharedKeyField.setErrorMessage("The shared key cannot be empty");
						sharedKeyField.setInvalid(true);
					} else {
						flyCommands.doSetSharedKey(sharedKeyField.getValue());
					}
				});

		Button restartAppsButton = new Button("Restart Apps",
				e -> {
					flyCommands.restartAllApps();
				});

		controlsLayout.add(sharedKeyField, generateKeyButton, setSecretsButton, restartAppsButton);
		contentDiv.add(controlsLayout);
		sharedKeySection.add(contentDiv);

		Div wrapper = new Div(sharedKeySection);
		apps.add(wrapper);
	}

	private void showApps(Map<AppType, App> appMap, VerticalLayout apps) {
		if (intro != null) {
			intro.setVisible(false);
		}
		App owlcmsApp = appMap.get(AppType.OWLCMS);
		App publicApp = appMap.get(AppType.PUBLICRESULTS);
		App trackerApp = appMap.get(AppType.TRACKER);

		// apps.getStyle().set("margin-top", "1em");
		apps.add(new Hr());

		Div showOwlcmsApp;
		if (owlcmsApp != null) {
			showOwlcmsApp = showApplication(owlcmsApp);
			apps.add(showOwlcmsApp);
		} else {
			owlcmsApp = new App("", AppType.OWLCMS, getCurrentRegion(), "stable", null, null);
			owlcmsApp.setVersionInfo(new VersionInfo("stable"));
			showOwlcmsApp = showApplication(owlcmsApp);
			apps.add(showOwlcmsApp);
		}

		Div showTrackerApp;
		apps.add(new Hr());

		if (trackerApp != null) {
			showTrackerApp = showApplication(trackerApp);
			apps.add(showTrackerApp);
		} else {
			trackerApp = new App("", AppType.TRACKER, getCurrentRegion(), "stable", null, null);
			trackerApp.setVersionInfo(
					new VersionInfo("stable", "https://api.github.com/repos/jflamy/owlcms-tracker/releases"));
			showTrackerApp = showApplication(trackerApp);
			apps.add(showTrackerApp);
		}

		Div showPublicApp;
		apps.add(new Hr());

		if (publicApp != null) {
			showPublicApp = showApplication(publicApp);
			apps.add(showPublicApp);
		} else {
			publicApp = new App("", AppType.PUBLICRESULTS, getCurrentRegion(), "stable", null, null);
			publicApp.setVersionInfo(new VersionInfo("stable"));
			showPublicApp = showApplication(publicApp);
			apps.add(showPublicApp);
		}

		// Show shared key section after TRACKER
		apps.add(new Hr());
		showSharedKey(apps);
	}

	private String getCurrentRegion() {
		return null;
	}

	// Define printable characters
	private static final String PRINTABLE_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()-_=+[]{}|;:,.<>?";

	public String generateRandomString(int length) {
		SecureRandom random = new SecureRandom();
		StringBuilder stringBuilder = new StringBuilder(length);

		for (int i = 0; i < length; i++) {
			int randomIndex = random.nextInt(PRINTABLE_CHARACTERS.length());
			char randomChar = PRINTABLE_CHARACTERS.charAt(randomIndex);
			stringBuilder.append(randomChar);
		}

		return stringBuilder.toString();
	}

	private String getLatestReleaseVersion(AppType appType) {
		if (appType == null || appType.releaseApiUrl == null) {
			return "unknown";
		}
		return switch (appType) {
			case TRACKER -> {
				if (cachedTrackerVersion == null) {
					cachedTrackerVersion = VersionInfo.fastFetchLatestReleaseVersion(appType.releaseApiUrl);
				}
				yield cachedTrackerVersion;
			}
			default -> {
				if (cachedOwlcmsVersion == null) {
					cachedOwlcmsVersion = VersionInfo.fastFetchLatestReleaseVersion(appType.releaseApiUrl);
				}
				yield cachedOwlcmsVersion;
			}
		};
	}
}
