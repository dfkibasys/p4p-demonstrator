package de.dfki.cos.basys.p4p.controlcomponent.wallet;

import java.io.StringReader;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dfki.cos.basys.common.component.ComponentContext;
import de.dfki.cos.basys.common.component.ServiceProvider;
import edu.wpi.rail.jrosbridge.Ros;
import edu.wpi.rail.jrosbridge.Topic;
import edu.wpi.rail.jrosbridge.ActionClient;
import edu.wpi.rail.jrosbridge.Goal;
import edu.wpi.rail.jrosbridge.Goal.GoalStatusEnum;
import edu.wpi.rail.jrosbridge.JRosbridge.WebSocketType;
import edu.wpi.rail.jrosbridge.callback.ActionCallback;
import edu.wpi.rail.jrosbridge.callback.TopicCallback;
import edu.wpi.rail.jrosbridge.messages.Message;
import edu.wpi.rail.jrosbridge.messages.actionlib.GoalStatus;
import edu.wpi.rail.jrosbridge.messages.geometry.Point;
import edu.wpi.rail.jrosbridge.messages.geometry.PointStamped;
import edu.wpi.rail.jrosbridge.messages.geometry.Pose;
import edu.wpi.rail.jrosbridge.messages.geometry.PoseStamped;
import edu.wpi.rail.jrosbridge.messages.geometry.Quaternion;
import edu.wpi.rail.jrosbridge.messages.sensor.BatteryState;
import edu.wpi.rail.jrosbridge.messages.std.Header;
import edu.wpi.rail.jrosbridge.primitives.Time;

public class WalletServiceImpl implements WalletService, ServiceProvider<WalletService> {

	private Properties config = null;

	private Ros ros = null;	
	private String protocol = "ws";
	private String host = "192.168.1.100";
	private int port = 9090;

	private ActionClient gotoClient;
	private ActionClient liftClient;

	private GoalStatusEnum gotoStatus = GoalStatusEnum.PENDING;
	private GoalStatusEnum liftStatus = GoalStatusEnum.PENDING;
	
	private final Logger LOGGER;
	
	public WalletServiceImpl(Properties config) {
		this.config = config;
		LOGGER =  LoggerFactory.getLogger("WalletServiceImpl");
	}
	
	@Override
	public boolean connect(ComponentContext context, String connectionString) {
		String patternString = "(?<protocol>wss?):\\/\\/(?<host>.*):(?<port>\\d*)";

		Pattern pattern = Pattern.compile(patternString);

		Matcher matcher = pattern.matcher(connectionString);
		boolean matches = matcher.matches();
		if (!matches) {
			//throw new ComponentException("Invalid external connection string! " + connectionString + " does not match the expected pattern " + patternString);
		}
		
		protocol = matcher.group("protocol");
		host = matcher.group("host");
		port = Integer.parseInt(matcher.group("port"));
				
		//TODO: support also wss
		ros = new Ros(host, port, WebSocketType.valueOf(protocol));
		
		if (ros.connect()) {
			String gotoServerName = config.getProperty("gotoServerName");
			String gotoActionName = config.getProperty("gotoActionName");
			gotoClient = new ActionClient(ros, gotoServerName, gotoActionName);
			gotoClient.initialize();
			
			String liftServerName = config.getProperty("liftServerName");
			String liftActionName = config.getProperty("liftActionName");
			liftClient = new ActionClient(ros, liftServerName, liftActionName);
			liftClient.initialize();
		}
		
//		boolean telemetryEnabled = false;
//		if (telemetryEnabled) {
//			Topic poseTopic = new Topic(ros, "/robot_pose", "geometry_msgs/PoseStamped");
//			poseTopic.subscribe(new TopicCallback() {
//				@Override
//				public void handleMessage(Message message) {
//					LOGGER.info("POSITION: " + message.toString());
//				}
//			});
//			
//			Topic batteryTopic = new Topic(ros, "/mobility_base/battery", "mobility_base_core_msgs/BatteryState");
//			batteryTopic.subscribe(new TopicCallback() {
//				@Override
//				public void handleMessage(Message message) {
//					LOGGER.info("BATTERY: " + message.toString());
//				}
//			});
//		}
		
		return ros.isConnected();

	}

	@Override
	public void disconnect() {
		if (ros.isConnected()) {
			gotoClient.dispose();
			liftClient.dispose();
			ros.disconnect();	
		}
	}

	@Override
	public boolean isConnected() {
		return ros == null ? false : ros.isConnected();
	}

	@Override
	public WalletService getService() {
		return this;
	}

	@Override
	public void gotoSymbolicPosition(String positionName) {		
		/*
		{
			"position": {
				"y": 2.118885089546935,
				"x": 6.438778587249939,
				"z": 0.1016
			},
			"orientation": {
				"y": 0.0,
				"x": 0.0,
				"z": -0.7111406609156684,
				"w": 0.7030497566975087
			}
		}
		 */

		gotoStatus = GoalStatusEnum.PENDING;
		
		Goal goal = gotoClient.createGoal(new ActionCallback() {
			
			@Override
			public void handleStatus(GoalStatus goalStatus) {
				LOGGER.debug("GOTO-STATUS: " + goalStatus.toString());	
				gotoStatus = GoalStatusEnum.get(goalStatus.getStatus());	
			}
			
			@Override
			public void handleResult(JsonObject result) {
				LOGGER.debug("GOTO-RESULT: " + result.toString());								
			}
			
			@Override
			public void handleFeedback(JsonObject feedback) {
				LOGGER.debug("GOTO-FEEDBK: " + feedback.toString());				
			}
		});
		
		PoseStamped poseOutOfMap = new PoseStamped(
				new Header(0, new Time(), "/map"), 
				new Pose(
					new Point(2.118885089546935, 6.438778587249939,  0.1016), 
					new Quaternion(0.0, 0.0, -0.7111406609156684, 0.7030497566975087)
				)
			);
		
		PoseStamped pose = new PoseStamped(
			new Header(0, new Time(), "/map"), 
			new Pose(
				new Point(6.438778587249939, 2.118885089546935, 0.1016), 
				new Quaternion(0.0, 0.0, -0.7111406609156684, 0.7030497566975087)
			)
		);
	
		
		JsonObject targetPose = Json.createObjectBuilder().add("target_pose", poseOutOfMap.toJsonObject()).build();
		goal.submit(targetPose);
	}

	@Override
	public void moveLiftToHeight(double height) {
		LOGGER.debug("Move lift to height %d.", height);
		
		liftStatus = GoalStatusEnum.PENDING;
		
		Goal goal = liftClient.createGoal(new ActionCallback() {
			
			@Override
			public void handleStatus(GoalStatus goalStatus) {
				LOGGER.debug("LIFT-STATUS: " + goalStatus.toString());	
				liftStatus = GoalStatusEnum.get(goalStatus.getStatus());	
			}
			
			@Override
			public void handleResult(JsonObject result) {
				LOGGER.debug("LIFT-RESULT: " + result.toString());								
			}
			
			@Override
			public void handleFeedback(JsonObject feedback) {
				LOGGER.debug("LIFT-FEEDBK: " + feedback.toString());				
			}
		});
		
		PointStamped ps = new PointStamped(
				
				new Header(0, new Time(), "/wallet_lift"), 
				
				new Point(0, 0, height)
			
			);
		
		JsonObject targetHeight = Json.createObjectBuilder().add("target_height", ps.toJsonObject()).build();
		goal.submit(targetHeight);
	}
	
	@Override
	public GoalStatusEnum getGotoStatus() {		
		return gotoStatus;
	}

	@Override
	public GoalStatusEnum getLiftStatus() {		
		return liftStatus;
	}
}
