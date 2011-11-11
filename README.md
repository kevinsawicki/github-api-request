# GitHub API Request
[GitHub Java API](https://github.com/eclipse/egit-github/tree/master/org.eclipse.egit.github.core) client implementation
using the [http-request](https://github.com/kevinsawicki/http-request) library.

## Usage

```java
HttpRequestClient client = new HttpRequestClient();
RepositoryService service = new RepositoryService(client);
for (Repository repo : service.getRepositories("defunkt"))
  System.out.println(repo.getName());
```
