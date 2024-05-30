# JPA Search Helper

### Description
Library for building advanced and dynamic queries using JPA in Spring.

Do you want your controller to be able to receive a request like this and perform an advanced and complex search?

```bash
curl - request GET \
 - url 'https://www.myexampledomain.com/persons?
firstName=Biagio
&lastName_startsWith=Toz
&birthDate_gte=19910101
&country_in=IT,FR,DE
&company.name_in=Microsoft,Apple
&company.employees_between=500,5000'
```

Read this readme!

### Prerequisites
- Java 17 or later
- Spring Boot 2.7.x

### Project dependency
#### Maven
```
<dependency>
    <groupId>app.tozzi</groupId>
    <artifactId>jpa-search-helper</artifactId>
    <version>1.0.2.GISGRO</version>
</dependency>
```

#### Gradle
```
implementation 'app.tozzi:jpa-search-helper:1.0.2.GISGRO'
```

### Managed search filters

| Filter name               | Library Key  | SQL                                    | Fixed value 
|---------------------------|--------------|----------------------------------------|---------------|
| Equals                    | _eq          | sql_col = val                          
| Equals (ignore case)      | _iEq         | UPPER(sql_col) = 'VAL'                 
| Contains                  | _contains    | sql_col LIKE '%val%'                   
| Contains (ignore case)    | _iContains   | UPPER(sql_col) LIKE '%VAL%'            
| In                        | _in          | sql_col IN (val1, val2, ..., valN)     
| Not In                    | _nin         | sql_col NOT IN (val1, val2, ..., valN) 
| Starts With               | _startsWith  | sql_col LIKE 'val%'                    
| Starts With (ignore case) | _iStartsWith | UPPER(sql_col) LIKE 'VAL%'             
| Ends With                 | _endsWith    | sql_col LIKE '%val'                    
| Ends With (ignore case)   | _iEndsWith   | UPPER(sql_col) LIKE '%VAL'             
| Not Equals                | _notEq       | sql_col <> val                         
| Not Equals (ignore case)  | _iNotEq      | UPPER(sql_col) <> 'VAL'                
| Greater Than              | _gt          | sql_col > val                          
| Greater Than or Equal     | _gte         | sql_col >= val                         
| Less Than                 | _lt          | sql_col < val                          
| Less Than or Equal        | _lte         | sql_col <= val                         
| Between                   | _between     | sql_col BETWEEN val1 AND val2          
| Null                      | _is | sql_col IS NULL                        | 'null'
| Empty                     | _is |                       | 'empty'
| Not Null                  | _is | sql_col IS NOT NULL                    | 'not_null'
| Not Empty                 | _is |                   | 'not_empty'


### Pagination

| Filter name | Key | Fixed values
|--|--|--|
| Limit (page size) | _limit
| Offset (page number) | _offset
| Sort | _sort | ASC, DESC

### Usage

#### Searchable annotation
Start by applying the `@Searchable` annotation to the fields in your DTO, or alternatively your JPA entity, that you want to make available for search.

```java
@Data
public class Person {

    @Searchable
    private String firstName;

    @Searchable
    private String lastName;

    @Searchable(entityFieldKey = "dateOfBirth")
    private Date birthDate;

    @Searchable
    private String country;

    @NestedSearchable
    private Company company;

    @Data
    public static class Company {
        
        @Searchable(entityFieldKey= "companyEntity.name")
        private String name;
              
        @Searchable(entityFieldKey= "companyEntity.employeesCount")
        private int employees;
    }
}
```

The annotation allows you to specify:

- Core properties:

  - *entityFieldKey*:
    the name of the field defined on the entity bean (not to be specified if using the annotation on the entity bean). If not specified the key will be the field name.
  - *targetType*: the managed object type by entity. If not specified the librariy tries to obtain it based on field type (es. Integer field without target type definition will be INTEGER). If there is no type compatible with those managed, it will be managed as a string. Managed types: 
  
      - STRING, INTEGER, DOUBLE, FLOAT, LONG, BIGDECIMAL, BOOLEAN, DATE, LOCALDATE, LOCALDATETIME, LOCALTIME, OFFSETDATETIME, OFFSETTIME.

- Validation properties:
  
  - *datePattern*: only for DATE targetType. Defines the date pattern to use.
  - *maxSize, minSize*: maximum/minimum length of the value
  - *maxDigits, minDigits*: only for numeric types. Maximum/minimum number of digits.
  - *regexPattern*: regex pattern.
  - *decimalFormat*: only for decimal numeric types. Default #.##

- Other:

  - *trim*: apply trim
  - *tags*: useful if the DTO field can correspond to multiple entity fields
  - *allowedFilters*: exclusively allowed filters
  - *notAllowedFilters*: not allowed filters
  - *likeFilters*: allowed like filters (_contains, _iContains, _startsWith, _iStartsWith, _endsWith, _iEndsWith). Default: true
  

Exceptions:
- If a field does not exist or is not searchable you will receive an `InvalidFieldException`.
- If the value of a field does not meet the requirements you will receive an `InvalidValueException`.

Continuing the example, our entity classes:

```java
@Entity
@Data
public class PersonEntity {

    @Id
    private Long id;

    @Column(name = "FIRST_NAME")
    private String firstName;

    @Column(name = "LAST_NAME")
    private String lastName;

    @Column(name = "BIRTH_DATE")
    private Date dateOfBirth;

    @Column(name = "COUNTRY")
    private String country;
        
    @OneToOne
    private CompanyEntity companyEntity;

}

@Entity
@Data
public class CompanyEntity {

    @Id
    private Long id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "COUNT")
    private Integer employeesCount;

}
```

#### JPASearchRepository
Your Spring JPA repository must extend JPASearchRepository<?>.

```java
@Repository
public interface PersonRepository extends JpaRepository<PersonEntity, Long>, JPASearchRepository<PersonEntity> {

}
```

In your service/manager bean define a map <filter_key, value>:
```java
// ...

Map<String, String> filters = new HashMap<>();
filters.put("firstName_eq", "Biagio");
filters.put("lastName_startsWith", "Toz");
filters.put("birthDate_gte", "19910101"); 
filters.put("country_in", "IT,FR,DE");
filters.put("company.name_in", "Microsoft,Apple");
filters.put("company.employees_between", "500,5000");

// Without pagination
List<PersonEntity> fullSearch = personRepository.findAll(filters, Person.class);

filters.put("birthDate_sort" : "ASC");
filters.put("_limit", "10");
filters.put("_offset", "0");

// With pagination
Page<PersonEntity> sortedAndPaginatedSearch = personRepository.findAllWithPaginationAndSorting(filters, Person.class);

// ...
```

End.

### Spring Boot Project example with HTTP Endpoint
Controller:
```java
@RestController
public class MyController {
    
    @Autowired 		
    private PersonManager personManager;
         
    @GetMapping(path="/persons", produces = MediaType.APPLICATION_JSON_VALUE)  
    public List<Person> findPersons(@RequestParam Map<String, String> requestParams) {  
        return personManager.find(requestParams);  
    }
}
```

Service/Manager bean:
```java
@Service 
public class PersonManager { 	
    	
    @Autowired 		
    private PersonRepository personRepository;
    		 		
    public List<Person> find(Map<String, String> filters) { 		
	return personRepository.findAllWithPaginationAndSorting(filters, Person.class).stream().map(this::toDTO).toList(); 
    } 

    private static Person toDTO(PersonEntity personEntity) {
        // ...
    }

}
```
Curl:
```bash
curl - request GET \
 - url 'https://www.myexampledomain.com/persons?
firstName=Biagio
&lastName_startsWith=Toz
&birthDate_gte=19910101
&country_in=IT,FR,DE
&company.name_in=Microsoft,Apple
&company.employees_between=500,5000'
```

### Join Fetch
It is possible to force joins with fetch to allow Hibernate to execute a single query for the relationships defined on the entity. **This is only possible without pagination**:
```java
// ...

Map<String, JoinFetch> fetches = Map.of("companyEntity", JoinFetch.LEFT);
personRepository.findAll(filters, Person.class, fetches);

// ...
```

### Multiple entities for the same DTO
If you have a DTO that is the result of the conversion of multiple entities, it is possible to explicitly specify a map (string, string) whose key represents the name of the DTO field and the value is the name of the field of the entity to be searched for:
```java
// ...

Map<String, String> entityFieldMap = Map.of("company", "companyEntity.name");

// Without pagination
personRepository.findAll(filters, Person.class, fetches, entityFieldMap);

// With pagination
personRepository.findAllWithPaginationAndSorting(filters, Person.class, entityFieldMap);

// ...
```

### Multiple object for the same Entity
Another special case could be where an object can be repeated within the DTO to represent multiple pieces of the entity. The solution for the search:
```java

@Entity
public class CoupleEntity {

	@Id
	private Long id;

	@Column(name = "p1_fn")
	private String p1FirstName;

	@Column(name = "p1_ln")
	private String p1LastName;

	@Column(name = "p2_fn")
	private String p2FirstName;

	@Column(name = "p2_ln")
	private String p2LastName;
}

@Data
public class CoupleDTO {

	@NestedSearchable
	private Person p1;

	@NestedSearchable
	private Person p2;

	@Data
	public static class Person {

		@Searchable(tags = {
	            @Tag(fieldKey = "p1.firstName", entityFieldKey = "p1FirstName"),
	            @Tag(fieldKey = "p2.firstName", entityFieldKey = "p2FirstName"),
    		})
		private String firstName;

		@Searchable(tags = {
	            @Tag(fieldKey = "p1.lastName", entityFieldKey = "p1LastName"),
	            @Tag(fieldKey = "p2.lastName", entityFieldKey = "p2LastName"),
    		})
		private String lastName;
	}
}
```

```bash
curl - request GET \
 - url 'https://www.myexampledomain.com/couples?
p1.firstName_iEq=Romeo
&p2.firstName_iEq=Giulietta'
```
