package de.dfki.cos.basys.p4p.controlcomponent.drone;

import de.dfki.cos.basys.controlcomponent.annotation.Parameter;
import de.dfki.cos.basys.controlcomponent.impl.BaseControlComponent;
import de.dfki.cos.basys.controlcomponent.impl.BaseOperationMode;
import de.dfki.cos.basys.p4p.controlcomponent.drone.service.DroneService;

import java.sql.Date;
import java.util.concurrent.TimeUnit;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import de.dfki.cos.basys.controlcomponent.ExecutionCommand;
import de.dfki.cos.basys.controlcomponent.ExecutionMode;
import de.dfki.cos.basys.controlcomponent.ParameterDirection;
import de.dfki.cos.basys.controlcomponent.annotation.OperationMode;

@OperationMode(name = "DetectObstacles", shortName = "DETECT", description = "detect nearby obstacles", 
		allowedCommands = {	ExecutionCommand.HOLD, ExecutionCommand.RESET, ExecutionCommand.START, ExecutionCommand.STOP }, 
		allowedModes = { ExecutionMode.PRODUCTION, ExecutionMode.SIMULATION })
public class DetectObstacleOperationMode extends BaseDroneOperationMode {
	@Parameter(name = "type", direction = ParameterDirection.IN)
	private String type = "block";	
	

	public DetectObstacleOperationMode(BaseControlComponent<DroneService> component) {
		super(component);
	}

	@Override
	public void onStarting() {
		// Start Video Streaming with endpoint of obstacle detection service
		getService(DroneService.class).startVideoStream();
		sleep(1000);
		getService(DroneService.class).detectObstacles(type);
		
	}

	@Override
	public void onCompleting() {
		// TODO Report retrieved set of obstacles to Worldmodel server
		sleep(1000);
		// Stop video stream
		getService(DroneService.class).stopVideoStream();
		getService(DroneService.class).reset();
		
	}

	@Override
	public void onStopping() {		
		getService(DroneService.class).stopVideoStream();
		getService(DroneService.class).reset();		
	}
}
