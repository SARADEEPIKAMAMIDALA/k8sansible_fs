package com.toursandtravel.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.toursandtravel.dto.CommonApiResponse;
import com.toursandtravel.dto.RegisterUserRequestDto;
import com.toursandtravel.dto.UserDto;
import com.toursandtravel.dto.UserLoginRequest;
import com.toursandtravel.dto.UserLoginResponse;
import com.toursandtravel.dto.UserResponseDto;
import com.toursandtravel.entity.Address;
import com.toursandtravel.entity.Tour;
import com.toursandtravel.entity.User;
import com.toursandtravel.exception.UserSaveFailedException;
import com.toursandtravel.service.AddressService;
import com.toursandtravel.service.TourService;
import com.toursandtravel.service.UserService;
import com.toursandtravel.utility.Constants.ActiveStatus;
import com.toursandtravel.utility.Constants.UserRole;
import com.toursandtravel.utility.JwtUtils;

import jakarta.transaction.Transactional;

@Component
@Transactional
public class UserResource {

	private final Logger LOG = LoggerFactory.getLogger(UserResource.class);

	@Autowired
	private UserService userService;

	@Autowired
	private AddressService addressService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JwtUtils jwtUtils;

	@Autowired
	private TourService tourService;

	public ResponseEntity<CommonApiResponse> registerAdmin(RegisterUserRequestDto registerRequest) {

		LOG.info("Request received for Register Admin");

		CommonApiResponse response = new CommonApiResponse();

		if (registerRequest == null) {
			response.setResponseMessage("user is null");
			response.setSuccess(false);
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		if (registerRequest.getEmailId() == null || registerRequest.getPassword() == null) {
			response.setResponseMessage("missing input");
			response.setSuccess(false);
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		User existingUser = this.userService.getUserByEmailAndStatus(registerRequest.getEmailId(),
				ActiveStatus.ACTIVE.value());

		if (existingUser != null) {
			response.setResponseMessage("User already registered with this Email");
			response.setSuccess(false);
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		User user = RegisterUserRequestDto.toUserEntity(registerRequest);
		user.setRole(UserRole.ROLE_ADMIN.value());
		user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
		user.setStatus(ActiveStatus.ACTIVE.value());

		existingUser = this.userService.addUser(user);

		if (existingUser == null) {
			response.setResponseMessage("failed to register admin");
			response.setSuccess(false);
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		response.setResponseMessage("Admin registered Successfully");
		response.setSuccess(true);
		LOG.info("Response Sent!!!");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	public ResponseEntity<CommonApiResponse> registerUser(RegisterUserRequestDto request) {

		LOG.info("Received request for register user");

		CommonApiResponse response = new CommonApiResponse();

		if (request == null) {
			response.setResponseMessage("user is null");
			response.setSuccess(false);
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		User existingUser = this.userService.getUserByEmailAndStatus(request.getEmailId(), ActiveStatus.ACTIVE.value());

		if (existingUser != null) {
			response.setResponseMessage("User with this Email Id already registered!!!");
			response.setSuccess(false);
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		if (request.getRole() == null) {
			response.setResponseMessage("bad request, Role is missing");
			response.setSuccess(false);
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		User user = RegisterUserRequestDto.toUserEntity(request);

		String encodedPassword = passwordEncoder.encode(user.getPassword());
		user.setStatus(ActiveStatus.ACTIVE.value());
		user.setPassword(encodedPassword);

		Address address = new Address();
		address.setCity(request.getCity());
		address.setPincode(request.getPincode());
		address.setStreet(request.getStreet());

		Address savedAddress = this.addressService.addAddress(address);
		if (savedAddress == null) {
			throw new UserSaveFailedException("Registration Failed because of Technical issue :(");
		}

		user.setAddress(savedAddress);
		existingUser = this.userService.addUser(user);

		if (existingUser == null) {
			throw new UserSaveFailedException("Registration Failed because of Technical issue :(");
		}

		response.setResponseMessage("User registered Successfully");
		response.setSuccess(true);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	public ResponseEntity<UserLoginResponse> login(UserLoginRequest loginRequest) {

		LOG.info("Received request for User Login");

		UserLoginResponse response = new UserLoginResponse();

		if (loginRequest == null) {
			response.setResponseMessage("Missing Input");
			response.setSuccess(false);
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		String jwtToken;
		User user;

		List<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority(loginRequest.getRole()));

		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(loginRequest.getEmailId(), loginRequest.getPassword(), authorities));
		} catch (Exception ex) {
			response.setResponseMessage("Invalid email or password.");
			response.setSuccess(false);
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		jwtToken = jwtUtils.generateToken(loginRequest.getEmailId());

		user = this.userService.getUserByEmailIdAndRoleAndStatus(
				loginRequest.getEmailId(),
				loginRequest.getRole(),
				ActiveStatus.ACTIVE.value()
		);

		if (user == null) {
			response.setResponseMessage("User not found or inactive");
			response.setSuccess(false);
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		UserDto userDto = UserDto.toUserDtoEntity(user);

		response.setUser(userDto);
		response.setResponseMessage("Logged in successfully");
		response.setSuccess(true);
		response.setJwtToken(jwtToken);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	public ResponseEntity<UserResponseDto> getUsersByRole(String role) {
		UserResponseDto response = new UserResponseDto();

		if (role == null) {
			response.setResponseMessage("missing role");
			response.setSuccess(false);
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		List<User> users = this.userService.getUserByRoleAndStatus(role, ActiveStatus.ACTIVE.value());

		if (users.isEmpty()) {
			response.setResponseMessage("No Users Found");
			response.setSuccess(false);
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		List<UserDto> userDtos = new ArrayList<>();
		for (User user : users) {
			userDtos.add(UserDto.toUserDtoEntity(user));
		}

		response.setUsers(userDtos);
		response.setResponseMessage("User Fetched Successfully");
		response.setSuccess(true);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	public ResponseEntity<UserResponseDto> getUserById(int userId) {
		UserResponseDto response = new UserResponseDto();

		if (userId == 0) {
			response.setResponseMessage("Invalid Input");
			response.setSuccess(false);
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		User user = this.userService.getUserById(userId);
		if (user == null) {
			response.setResponseMessage("User not found");
			response.setSuccess(false);
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		List<UserDto> userDtos = new ArrayList<>();
		userDtos.add(UserDto.toUserDtoEntity(user));

		response.setUsers(userDtos);
		response.setResponseMessage("User Fetched Successfully");
		response.setSuccess(true);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	public ResponseEntity<CommonApiResponse> deleteGuide(Integer guideId) {

		LOG.info("Received request for deleting the Guide");

		CommonApiResponse response = new CommonApiResponse();

		if (guideId == null) {
			response.setResponseMessage("bad request, missing data");
			response.setSuccess(false);
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		User user = this.userService.getUserById(guideId);
		if (user == null) {
			response.setResponseMessage("Guide not found");
			response.setSuccess(false);
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		user.setStatus(ActiveStatus.DEACTIVATED.value());
		User updatedUser = this.userService.updateUser(user);

		if (updatedUser == null) {
			throw new UserSaveFailedException("Failed to delete tour guide!!!");
		}

		List<Tour> guideTours = this.tourService.getAllByGuide(updatedUser);
		for (Tour tour : guideTours) {
			tour.setStatus(ActiveStatus.DEACTIVATED.value());
			this.tourService.updateTour(tour);
		}

		response.setResponseMessage("Tour Guide and all its Tours deleted successfully!!!");
		response.setSuccess(true);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
