# Architecture Diagram Guide

This guide explains how to view and edit the Farmatodo microservices architecture diagrams.

## Files Included

1. **architecture-diagram.drawio** - Draw.io/diagrams.net format (recommended for detailed editing)
2. **architecture-diagram.mmd** - Mermaid format (text-based, GitHub-compatible)

## Architecture Overview

The diagrams show:

### 1. Configuration Management Layer
- **Config Service** (Git Repository) - Stores configuration files
- **Config Server** (Spring Cloud Config) - Distributes configuration to all microservices
- All microservices connect to Config Server for centralized configuration

### 2. Microservices Layer
- **API Gateway** (:8080) - Entry point for all client requests
- **Client Service** (:8081) - Client management with PostgreSQL
- **Token Service** (:8082) - Token and payment processing with PostgreSQL
- **Product Service** (:8083) - Product catalog with PostgreSQL
- **Cart Service** (:8084) - Shopping cart with PostgreSQL
- **Order Service** (:8085) - Order processing with PostgreSQL

### 3. CI/CD Pipeline (Jenkins)
The pipeline includes the following stages:
1. **Source Code** (GitHub) - Code repository
2. **Unit Tests** (Maven) - Automated testing
3. **SonarQube** - Code quality analysis
4. **Trivy** - Security vulnerability scanning
5. **Docker Build** - Container image creation
6. **Push to GCP Registry** - Image storage
7. **Deploy to Cloud Run** - Production deployment

### 4. Deployment Environments

#### Local Development
- **Docker Compose** - Orchestrates all services and databases locally
- Uses `docker-compose.yml` for local testing

#### Google Cloud Platform (Production)
- **Compute Engine** - Manages orchestration
- **Cloud Run** - Deploys containerized microservices with auto-scaling
- Each microservice runs as an independent Cloud Run service

---

## How to Use the Diagrams

### Option 1: Draw.io (Recommended for Editing)

**File:** `architecture-diagram.drawio`

#### Online (No Installation Required)

1. Visit [https://app.diagrams.net](https://app.diagrams.net)
2. Click **"Open Existing Diagram"**
3. Select **"Open from > Device"**
4. Choose `architecture-diagram.drawio`
5. The diagram will open in your browser

#### Desktop Application

1. Download Draw.io Desktop from [https://github.com/jgraph/drawio-desktop/releases](https://github.com/jgraph/drawio-desktop/releases)
2. Install the application
3. Open the application
4. Go to **File > Open** and select `architecture-diagram.drawio`

#### Editing the Diagram

- **Move elements**: Click and drag
- **Add elements**: Use the left sidebar or click **"+" > More Shapes**
- **Edit text**: Double-click on any element
- **Change colors**: Select element, then use the **Format Panel** on the right
- **Add connections**: Click on a shape's connection point and drag to another shape
- **Export**: Go to **File > Export as** (PNG, PDF, SVG, etc.)

### Option 2: Mermaid (For GitHub and Documentation)

**File:** `architecture-diagram.mmd`

#### View Online (No Installation Required)

1. Visit [https://mermaid.live](https://mermaid.live)
2. Click **"Load from file"** or paste the contents of `architecture-diagram.mmd`
3. The diagram will render automatically

#### View in GitHub

Simply create or edit a markdown file and include:

````markdown
```mermaid
[paste content from architecture-diagram.mmd here]
```
````

GitHub will automatically render the diagram.

#### View in VS Code

1. Install the **Mermaid Preview** extension
2. Open `architecture-diagram.mmd`
3. Press `Ctrl+Shift+V` (Windows/Linux) or `Cmd+Shift+V` (Mac) to preview

#### Editing the Mermaid Diagram

- The file is text-based, edit directly in any text editor
- Use [Mermaid documentation](https://mermaid.js.org/intro/) for syntax reference
- Changes are reflected immediately in the preview

---

## Customization Tips

### For Draw.io:

1. **Update Service Names**: Double-click on any box and edit the text
2. **Add New Services**:
   - Copy an existing service box (Ctrl+C, Ctrl+V)
   - Update the text and position
   - Connect to Config Server with a dashed line
3. **Change Colors**:
   - Select element
   - Use the color picker in the Format Panel
4. **Add Pipeline Stages**:
   - Copy an existing pipeline box
   - Add connection arrows between stages

### For Mermaid:

1. **Add New Microservice**:
   ```
   NewSvc["New Service<br/>:8086<br/>PostgreSQL"]
   ConfigServer -.->|"Config"| NewSvc
   ```

2. **Add Pipeline Stage**:
   ```
   NewStage["New Stage<br/>(Tool Name)"]
   PreviousStage --> NewStage
   NewStage --> NextStage
   ```

3. **Change Colors**:
   - Add to the classDef section at the bottom
   - Apply with: `class NewElement customStyle`

---

## Export Options

### Draw.io Export Formats:

- **PNG/JPG** - For presentations and documents
  - File > Export as > PNG/JPEG
  - Adjust resolution (100%, 200%, 300%)

- **PDF** - For high-quality printing
  - File > Export as > PDF

- **SVG** - For scalable web graphics
  - File > Export as > SVG

### Mermaid Export Options:

- **Via Mermaid Live Editor**:
  1. Open diagram at [mermaid.live](https://mermaid.live)
  2. Click the download icon
  3. Choose PNG or SVG

- **Via VS Code**:
  - With Mermaid Preview extension installed
  - Right-click on preview > "Save as PNG/SVG"

---

## Integration with Documentation

### Include in README.md:

#### For Draw.io:
1. Export as PNG: `architecture-diagram.png`
2. Add to README:
   ```markdown
   ## Architecture
   ![Architecture Diagram](architecture-diagram.png)
   ```

#### For Mermaid:
1. Copy content from `architecture-diagram.mmd`
2. Add to README:
   ````markdown
   ## Architecture
   ```mermaid
   [paste content here]
   ```
   ````

---

## Maintenance

### When Adding a New Service:

1. **Update Draw.io Diagram**:
   - Add new box in microservices layer
   - Connect to Config Server with dashed line
   - Update pipeline if needed
   - Export new version

2. **Update Mermaid Diagram**:
   - Add service definition in microservices subgraph
   - Add config connection: `ConfigServer -.->|"Config"| NewService`
   - Commit changes

3. **Keep Both in Sync**:
   - Always update both diagrams when architecture changes
   - Include diagram updates in your pull requests

---

## Additional Resources

### Draw.io:
- [Official Documentation](https://www.diagrams.net/doc/)
- [Tutorial Videos](https://www.youtube.com/c/drawio-app)
- [Shape Libraries](https://www.diagrams.net/blog/shape-libraries)

### Mermaid:
- [Official Documentation](https://mermaid.js.org/)
- [Syntax Reference](https://mermaid.js.org/intro/syntax-reference.html)
- [Live Editor](https://mermaid.live/)

---

## Troubleshooting

### Draw.io won't open the file:
- Ensure you're using the latest version
- Try opening in browser version first
- Check file isn't corrupted

### Mermaid diagram not rendering:
- Verify syntax is correct at [mermaid.live](https://mermaid.live)
- Check for missing quotes or brackets
- Ensure subgraph names are unique

### Changes not saving:
- **Draw.io**: File > Save or Ctrl+S
- **Mermaid**: Save the .mmd file in your text editor

---

## Questions or Issues?

If you have questions about the architecture or diagrams:
1. Refer to the main [CLAUDE.md](CLAUDE.md) for project details
2. Check [POSTMAN-GUIDE.md](POSTMAN-GUIDE.md) for API testing
3. Review CI/CD configuration in `Jenkinsfile.gcp-deploy`

---

**Note**: Keep these diagrams updated as the architecture evolves. They serve as crucial documentation for the team and stakeholders.
