package io.pivotal.cfapp.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.DatabaseClient.GenericInsertSpec;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import io.pivotal.cfapp.config.DbmsSettings;
import io.pivotal.cfapp.config.PoliciesSettings;
import io.pivotal.cfapp.domain.ApplicationOperation;
import io.pivotal.cfapp.domain.ApplicationPolicy;
import io.pivotal.cfapp.domain.Defaults;
import io.pivotal.cfapp.domain.Policies;
import io.pivotal.cfapp.domain.PoliciesValidator;
import io.pivotal.cfapp.domain.ServiceInstanceOperation;
import io.pivotal.cfapp.domain.ServiceInstancePolicy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class R2dbcPoliciesRepository {

	private final DatabaseClient client;
	private final PoliciesSettings policiesSettings;
	private final DbmsSettings dbmsSettings;
	private final ObjectMapper mapper;

	@Autowired
	public R2dbcPoliciesRepository(
		DatabaseClient client,
		PoliciesSettings policiesSettings,
		DbmsSettings dbmsSettings,
		ObjectMapper mapper) {
		this.client = client;
		this.policiesSettings = policiesSettings;
		this.dbmsSettings = dbmsSettings;
		this.mapper = mapper;
	}

	public Mono<Policies> save(Policies entity) {
		List<ApplicationPolicy> applicationPolicies = entity.getApplicationPolicies().stream()
				.filter(ap -> PoliciesValidator.validate(ap)).map(p -> seedApplicationPolicy(p)).collect(Collectors.toList());

		List<ServiceInstancePolicy> serviceInstancePolicies = entity.getServiceInstancePolicies().stream()
				.filter(sip -> PoliciesValidator.validate(sip)).map(p -> seedServiceInstancePolicy(p)).collect(Collectors.toList());

		return Flux.fromIterable(applicationPolicies)
					.flatMap(ap -> saveApplicationPolicy(ap))
					.thenMany(Flux.fromIterable(serviceInstancePolicies)
					.flatMap(sip -> saveServiceInstancePolicy(sip)))
					.then(Mono.just(new Policies(applicationPolicies, serviceInstancePolicies)));
	}

	public Mono<Policies> findServiceInstancePolicyById(String id) {
		String index = dbmsSettings.getBindPrefix() + 1;
		String selectServiceInstancePolicy = "select pk, id, operation, description, options, organization_whitelist from service_instance_policy where id = " + index;
		List<ServiceInstancePolicy> serviceInstancePolicies = new ArrayList<>();
		return
			Flux
				.from(client.execute().sql(selectServiceInstancePolicy)
						.bind(index, id)
						.map((row, metadata) ->
							ServiceInstancePolicy
								.builder()
									.pk(row.get("pk", Long.class))
									.id(row.get("id", String.class))
									.operation(row.get("operation", String.class))
									.description(Defaults.getValueOrDefault(row.get("description", String.class), ""))
									.options(readOptions(row.get("options", String.class) == null ? "{}" : row.get("options", String.class)))
									.organizationWhiteList(row.get("organization_whitelist", String.class) != null ? new HashSet<String>(Arrays.asList(row.get("organization_whitelist", String.class).split("\\s*,\\s*"))): new HashSet<>())
									.build())
						.all())
				.map(sp -> serviceInstancePolicies.add(sp))
				.then(Mono.just(new Policies(Collections.emptyList(), serviceInstancePolicies)))
				.flatMap(p -> p.isEmpty() ? Mono.empty(): Mono.just(p));
	}

	public Mono<Policies> findApplicationPolicyById(String id) {
		String index = dbmsSettings.getBindPrefix() + 1;
		String selectApplicationPolicy = "select pk, id, operation, description, state, options, organization_whitelist from application_policy where id = " + index;
		List<ApplicationPolicy> applicationPolicies = new ArrayList<>();
		return
			Flux
				.from(client.execute().sql(selectApplicationPolicy)
						.bind(index, id)
						.map((row, metadata) ->
							ApplicationPolicy
								.builder()
									.pk(row.get("pk", Long.class))
									.id(row.get("id", String.class))
									.operation(row.get("operation", String.class))
									.description(Defaults.getValueOrDefault(row.get("description", String.class), ""))
									.options(readOptions(row.get("options", String.class) == null ? "{}" : row.get("options", String.class)))
									.organizationWhiteList(row.get("organization_whitelist", String.class) != null ? new HashSet<String>(Arrays.asList(row.get("organization_whitelist", String.class).split("\\s*,\\s*"))): new HashSet<>())
									.state(row.get("state", String.class))
									.build())
						.all())
				.map(ap -> applicationPolicies.add(ap))
				.then(Mono.just(new Policies(applicationPolicies, Collections.emptyList())))
				.flatMap(p -> p.isEmpty() ? Mono.empty(): Mono.just(p));
	}

	public Mono<Policies> findAll() {
		String selectAllApplicationPolicies = "select pk, id, operation, description, state, options, organization_whitelist from application_policy";
		String selectAllServiceInstancePolicies = "select pk, id, operation, description, options, organization_whitelist from service_instance_policy";
		List<ApplicationPolicy> applicationPolicies = new ArrayList<>();
		List<ServiceInstancePolicy> serviceInstancePolicies = new ArrayList<>();

		return
				Flux
					.from(client.execute().sql(selectAllApplicationPolicies)
							.map((row, metadata) ->
								ApplicationPolicy
									.builder()
										.pk(row.get("pk", Long.class))
										.id(row.get("id", String.class))
										.operation(row.get("operation", String.class))
										.description(row.get("description", String.class))
										.options(readOptions(row.get("options", String.class) == null ? "{}" : row.get("options", String.class)))
										.organizationWhiteList(row.get("organization_whitelist", String.class) != null ? new HashSet<String>(Arrays.asList(row.get("organization_whitelist", String.class).split("\\s*,\\s*"))): new HashSet<>())
										.state(row.get("state", String.class))
										.build())
							.all())
					.map(ap -> applicationPolicies.add(ap))
					.thenMany(
						Flux
							.from(client.execute().sql(selectAllServiceInstancePolicies)
									.map((row, metadata) ->
										ServiceInstancePolicy
											.builder()
												.pk(row.get("pk", Long.class))
												.id(row.get("id", String.class))
												.operation(row.get("operation", String.class))
												.description(Defaults.getValueOrDefault(row.get("description", String.class), ""))
												.options(readOptions(row.get("options", String.class) == null ? "{}" : row.get("options", String.class)))
												.organizationWhiteList(row.get("organization_whitelist", String.class) != null ? new HashSet<String>(Arrays.asList(row.get("organization_whitelist", String.class).split("\\s*,\\s*"))): new HashSet<>())
												.build())
									.all())
							.map(sp -> serviceInstancePolicies.add(sp)))
					.then(Mono.just(new Policies(applicationPolicies, serviceInstancePolicies)))
					.flatMap(p -> p.isEmpty() ? Mono.empty(): Mono.just(p));
	}

	public Mono<Void> deleteApplicationPolicyById(String id) {
		String index = dbmsSettings.getBindPrefix() + 1;
		String deleteApplicationPolicy = "delete from application_policy where id = " + index;
		return
			Flux
				.from(client.execute().sql(deleteApplicationPolicy)
					.bind(index, id)
					.fetch()
					.rowsUpdated())
				.then();
	}

	public Mono<Void> deleteServicePolicyById(String id) {
		String index = dbmsSettings.getBindPrefix() + 1;
		String deleteServiceInstancePolicy = "delete from service_instance_policy where id = " + index;
		return
			Flux
				.from(client.execute().sql(deleteServiceInstancePolicy)
					.bind(index, id)
					.fetch()
					.rowsUpdated())
				.then();
	}

	public Mono<Void> deleteAll() {
		String deleteAllApplicationPolicies = "delete from application_policy";
		String deleteAllServiceInstancePolicies = "delete from service_instance_policy";
		return
			Flux
				.from(client.execute().sql(deleteAllApplicationPolicies)
					.fetch()
					.rowsUpdated())
				.thenMany(
					Flux
						.from(client.execute().sql(deleteAllServiceInstancePolicies)
							.fetch()
							.rowsUpdated()))
				.then();
	}

	private ApplicationPolicy seedApplicationPolicy(ApplicationPolicy policy) {
		return policiesSettings.isVersionManaged() ? ApplicationPolicy.seedWith(policy, policiesSettings.getCommit()): ApplicationPolicy.seed(policy);
	}

	private ServiceInstancePolicy seedServiceInstancePolicy(ServiceInstancePolicy policy) {
		return policiesSettings.isVersionManaged() ? ServiceInstancePolicy.seedWith(policy, policiesSettings.getCommit()): ServiceInstancePolicy.seed(policy);
	}

	private Mono<Integer> saveApplicationPolicy(ApplicationPolicy ap) {
		GenericInsertSpec<Map<String, Object>> spec =
			client.insert().into("application_policy")
				.value("id", ap.getId());
		if (ap.getDescription() != null) {
			spec = spec.value("description", ap.getDescription());
		} else {
			spec = spec.nullValue("description");
		}
		if (ap.getState() != null) {
			spec = spec.value("state", ap.getState());
		} else {
			spec = spec.nullValue("state");
		}
		if (ap.getOperation() != null) {
			spec = spec.value("operation", ap.getOperation());
		} else {
			spec = spec.nullValue("operation");
		}
		if (!CollectionUtils.isEmpty(ap.getOptions())) {
			spec = spec.value("options", writeOptions(ap.getOptions()));
		} else {
			spec = spec.nullValue("options");
		}
		spec = spec.value("organization_whitelist", String.join(",", ap.getOrganizationWhiteList()));
		return spec.fetch().rowsUpdated();
	}

	private Mono<Integer> saveServiceInstancePolicy(ServiceInstancePolicy sip) {
		GenericInsertSpec<Map<String, Object>> spec =
			client.insert().into("service_instance_policy")
				.value("id", sip.getId());
		if (sip.getDescription() != null) {
			spec = spec.value("description", sip.getDescription());
		} else {
			spec = spec.nullValue("description");
		}
		if (sip.getOperation() != null) {
			spec = spec.value("operation", sip.getOperation());
		} else {
			spec = spec.nullValue("operation");
		}
		if (!CollectionUtils.isEmpty(sip.getOptions())) {
			spec = spec.value("options", writeOptions(sip.getOptions()));
		} else {
			spec = spec.nullValue("options");
		}
		spec = spec.value("organization_whitelist", String.join(",", sip.getOrganizationWhiteList()));
		return spec.fetch().rowsUpdated();
	}

	public Mono<Policies> findByApplicationOperation(ApplicationOperation operation) {
		String index = dbmsSettings.getBindPrefix() + 1;
		String selectAllApplicationPolicies = "select pk, id, operation, description, state, options, organization_whitelist from application_policy where operation = " + index;
		List<ApplicationPolicy> applicationPolicies = new ArrayList<>();
		List<ServiceInstancePolicy> serviceInstancePolicies = new ArrayList<>();

		return
				Flux
					.from(client.execute().sql(selectAllApplicationPolicies).bind(index, operation.getName())
							.map((row, metadata) ->
								ApplicationPolicy
									.builder()
										.pk(row.get("pk", Long.class))
										.id(row.get("id", String.class))
										.operation(row.get("operation", String.class))
										.description(row.get("description", String.class))
										.options(readOptions(row.get("options", String.class) == null ? "{}" : row.get("options", String.class)))
										.organizationWhiteList(row.get("organization_whitelist", String.class) != null ? new HashSet<String>(Arrays.asList(row.get("organization_whitelist", String.class).split("\\s*,\\s*"))): new HashSet<>())
										.state(row.get("state", String.class))
										.build())
							.all())
					.map(ap -> applicationPolicies.add(ap))
					.then(Mono.just(new Policies(applicationPolicies, serviceInstancePolicies)))
					.flatMap(p -> p.isEmpty() ? Mono.empty(): Mono.just(p));
	}

	public Mono<Policies> findByServiceInstanceOperation(ServiceInstanceOperation operation) {
		String index = dbmsSettings.getBindPrefix() + 1;
		String selectAllServiceInstancePolicies = "select pk, id, operation, description, options, organization_whitelist from service_instance_policy where operation = " + index;
		List<ApplicationPolicy> applicationPolicies = new ArrayList<>();
		List<ServiceInstancePolicy> serviceInstancePolicies = new ArrayList<>();

		return
				Flux
					.from(client.execute().sql(selectAllServiceInstancePolicies).bind(index, operation.getName())
							.map((row, metadata) ->
								ServiceInstancePolicy
									.builder()
										.pk(row.get("pk", Long.class))
										.id(row.get("id", String.class))
										.operation(row.get("operation", String.class))
										.description(Defaults.getValueOrDefault(row.get("description", String.class), ""))
										.options(readOptions(row.get("options", String.class) == null ? "{}" : row.get("options", String.class)))
										.organizationWhiteList(row.get("organization_whitelist", String.class) != null ? new HashSet<String>(Arrays.asList(row.get("organization_whitelist", String.class).split("\\s*,\\s*"))): new HashSet<>())
										.build())
							.all())
					.map(sp -> serviceInstancePolicies.add(sp))
					.then(Mono.just(new Policies(applicationPolicies, serviceInstancePolicies)))
					.flatMap(p -> p.isEmpty() ? Mono.empty(): Mono.just(p));
	}

	private String writeOptions(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException jpe) {
            throw new RuntimeException("Problem writing options", jpe);
        }
	}

	private Map<String, Object> readOptions(String value) {
        try {
            return mapper.readValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (IOException ioe) {
            throw new RuntimeException("Problem reading options", ioe);
        }
	}
}
