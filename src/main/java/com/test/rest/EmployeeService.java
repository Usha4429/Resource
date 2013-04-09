package com.test.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.test.model.Employee;


/**
 * Service bean for Employee entities.
 * <p>
 * This class provides CRUD functionality for all Employee entities. It focuses
 * purely on Java EE 6 standards (e.g. <tt>&#64;ConversationScoped</tt> for
 * state management, <tt>PersistenceContext</tt> for persistence,
 * <tt>CriteriaBuilder</tt> for searches) rather than introducing a CRUD
 * framework or custom base class.
 */

@Path("/employee")
@Stateful
@RequestScoped
public class EmployeeService {

	@PersistenceContext
	private EntityManager entityManager;

	@Inject
	private Validator validator;

	/*
	 * Support retrieving Employee entities
	 */

	@GET
	@Path("/{id:[0-9][0-9]*}")
	@Produces(MediaType.APPLICATION_JSON)
	public Employee retrieve(@PathParam("id") long id) {

		return this.entityManager.find(Employee.class, id);
	}

	/*
	 * Support creating, updating and deleting Employee entities
	 */

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response create(Employee employee) {
		
		return update(null, employee);
	}

	@PUT
	@Path("/{id:[0-9][0-9]*}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response update(@PathParam("id") Long id, Employee employee) {

		employee.setId(id);
		
		// Validate

		Set<ConstraintViolation<Employee>> violations = this.validator.validate(employee);

		if (!violations.isEmpty()) {

			Map<String, String> responseObj = new HashMap<String, String>();

			for (ConstraintViolation<?> violation : violations) {
				responseObj.put(violation.getPropertyPath().toString(), violation.getMessage());
			}

			return Response.status(Response.Status.PRECONDITION_FAILED).entity(responseObj).build();
		}

		// Save
		
		if (id == null) {
			this.entityManager.persist(employee);
		} else {
			this.entityManager.merge(employee);
		}
		return Response.ok().build();
	}

	@DELETE
	@Path("/{id:[0-9][0-9]*}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") long id) {

		//this.entityManager.remove(retrieve(id));
		this.entityManager.createQuery("DELETE FROM Employee emp WHERE emp.id=:id").setParameter("id",id).executeUpdate();
		return Response.ok().build();
	}
	
	/*
	 * Support searching Employee entities
	 */
	 
	@POST
	@Path("/search")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public List<Employee> getPageItems(Employee search) {
		
		CriteriaBuilder builder = this.entityManager.getCriteriaBuilder();		
		CriteriaQuery<Employee> criteria = builder.createQuery(Employee.class);
		Root<Employee> root = criteria.from(Employee.class);
		criteria = criteria.select(root);
		
		if ( search != null ) {
			criteria = criteria.where(getSearchPredicates(root, search));
		}
		
		return this.entityManager.createQuery(criteria).getResultList();
	}

	private Predicate[] getSearchPredicates(Root<Employee> root, Employee search) {

		CriteriaBuilder builder = this.entityManager.getCriteriaBuilder();
		List<Predicate> predicatesList = new ArrayList<Predicate>();

		String firstName = search.getFirstName();
		if (firstName != null && !"".equals(firstName)) {
			predicatesList.add(builder.like(root.<String>get("firstName"), '%' + firstName + '%'));
		}
		String lastName = search.getLastName();
		if (lastName != null && !"".equals(lastName)) {
			predicatesList.add(builder.like(root.<String>get("lastName"), '%' + lastName + '%'));
		}
		

		return predicatesList.toArray(new Predicate[predicatesList.size()]);
	}

	/*
	 * Support listing and POSTing back Employee entities (e.g. from inside a
	 * &lt;select&gt;)
	 */

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Employee> getAll() {
		
		return getPageItems( null );
	}
}