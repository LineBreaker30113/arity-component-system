//# ==============================================================================
//# ONTO DIRECTORY - LIVING ARCHITECTURE & MANUAL SPECIFICATION
//# ==============================================================================
//# DEV NOTE: This file is parsed by ManualParser.java.
//# Lines starting with '//' are skipped by the GUI parser but remain here.
//# ==============================================================================

# Introduction to Onto Directory

````html
<b>With  newer lose your personal information or files again!</b><br>
Onto Directory is an elegant, highly structured environment built to safely organize long-term file archives.
<div style='color: #FFD700; margin-top: 8px;'><b>⚠ Paradigm Shift Notice:</b>
    This application does not use folders. It uses a Directed Acyclic Graph (DAG).
</div>
While standard applications assume you instantly understand their workflow, Onto Directory includes this guide to ensure you master its paradigm. Scan the summaries below, and expand the details only when you are ready to dive deeper into the mechanics.<br><br>
This guide covers every release of Onto Directory. Most of it applies everywhere unchanged; where the newer <b>nStructure engine preview</b> (launched separately, still early access) behaves differently from the current release, both are called out by name.
````

````html
Standard file managers force you to memorize rigid paths (e.g., <code>Archive/Work/2026/Invoice.pdf</code>),
causing friction when a file belongs in multiple categories. <br>
The ideology of <span style="color: #FFD700FF; font-weight: 700">Onto Directory</span>: Ontology is the philosophy of objects, and it studies their classifications and definitions.
If we view files as object, we can use all the studies and discoveries of Ontology (which is the peak of this field) about classifications to organize them. <br>
Powered by an <b>Ontology Engine</b>, Onto Directory eliminates folders entirely. You simply place files into a unified "Data Lake" and dynamically assign conceptual attributes (e.g., "Finance", "2026").
This allows you to cross-reference and retrieve data instantly, mirroring how the human brain actually categorizes information.
````

# 1. Getting Started: Your First Data Lake
````html
A Data Lake is the secure, isolated vault for your files. Think of it as a master database. You should aim to create one or two large Data Lakes (e.g., "Personal" and "Work") rather than many small ones.
````

````html
<b>To begin:</b> Click the <b>+ Create / Load Data Lake</b> button on the Welcome Page. Select an empty folder on your hard drive. Onto Directory will immediately initialize a secure <code>.DATA_LAKE</code> indexing structure inside it.<br><br>
Once loaded, your Lake will appear in the Left Sidebar. You can freely switch between Lakes, but only one can be active in your workspace at a time. To ensure your data is perfectly synchronized, always use the <b>Close</b> button on the Lake's header before exiting the application.<br><br>
<i>Safety net:</i> Onto Directory also saves automatically in the background a few seconds after any change, so you're still protected even if you forget to Close first.
````

# 2. Importing & Exporting Files
````html
Files must be ingested into the Data Lake before they can be tagged. Once inside, they are protected. When you need them outside the application, you extract a clean copy via Export.
````

````html
<b>Importing:</b> Click the <b>Import</b> button on your active Data Lake in the sidebar to open a folder picker and import a specific directory from your computer. <b>Right-click</b> the same button instead to import whatever files are already sitting in the <code>imports</code> folder, which Onto Directory generates <i>inside</i> the folder you chose for your Lake — handy if you'd rather drop files in via your OS file manager first. Note that this empties <code>imports</code> afterwards: the files are moved into the Lake, not copied. <br><br>
<i>Note:</i> When you import a directory, Onto Directory intelligently reads your existing folder structure and converts it into Tags automatically, saving you hours of manual organization.<br><br>
<b>Exporting:</b> Right-click any file in the File Explorer view and select <b>Export</b>. This will not delete the file from your Lake; it simply writes a pristine copy of the file to the destination of your choice.
````

# 3. The Graph: Tags vs. Folders
````html
Onto Directory uses a Directed Acyclic Graph (DAG). In this system, you do not put files "inside" tags. You assign tags "onto" files. This allows one file to exist in many contexts simultaneously.
````

````html
In a traditional file system, an invoice must live in either 'Finances' or 'Projects'. If you want it in both, you must create a shortcut or duplicate the file.<br><br>
With our ontology structure, you simply assign both tags to the file. <b>How to tag:</b>
<ul>
    <li>Select files in the File Explorer, right-click, and choose <b>Add Tag to Selected...</b>.</li>
    <li>Select files in the File Explorer, and drag them directly onto a Tag in the Hierarchy Tree.</li>
</ul>
Every file automatically inherits the root <b>"File"</b> tag.
````

# 4. Building the Hierarchy Tree
````html
Tags can be nested. If you assign the tag "Physics" to be a child of "Science", any file tagged with "Physics" will automatically appear when you search for "Science".
````

````html
The <b>Hierarchy Tree</b> view is your architect's canvas. Here you define the relationships between concepts.<br><br>
<b>Right-click</b> any tag in the tree to:
<ul>
    <li><b>Create Sub-Tag Here:</b> Generate a new child tag beneath it.</li>
    <li><b>Add Existing Tag as Child...:</b> Link an already existing tag underneath the current one. This allows a single tag to have multiple parents (e.g., 'Data Science' can be a child of both 'Math' and 'Computer Science').</li>
    <li><b>Add Parent... / Remove a Parent... / Remove a Child...:</b> Rewire the graph directly, for when drag-and-drop isn't precise enough.</li>
    <li><b>Rename Tag:</b> Changes the tag's display name without touching anything it's connected to.</li>
    <li><b>Copy Contents To...:</b> Copies a tag's parents, children, and/or tagged files onto another tag in one step.</li>
    <li><b>Delete Tag Completely:</b> Removes the tag. Any files holding only that tag will safely fall back to the root "File" category.</li>
</ul>
<i>Selecting several tags at once</i> (hold <b>Ctrl</b>) changes the menu rather than extending it: the single-tag entries above are replaced by one <b>Delete All Selected Tags</b> action. Its dialog warns that this "cannot be undone in bulk" — meaning there is no single undo for the whole batch, not that the deletions are lost. Each tag is recorded individually, so in the <b>current release</b> pressing <b>Ctrl+Z</b> once per tag brings them all back. Deleting a large batch does consume one history slot per tag, which matters against the limits in Section 7.<br>
<span style='color:#FF8888;'><b>Known issue — nStructure engine preview:</b> undoing the deletion of a <i>top-level</i> tag (one whose only parent is the root "File") currently fails. Deleting nested tags undoes correctly. Until this is fixed, treat deletion of a top-level tag as permanent in the preview build.</span><br>
The same right-click menu also offers <b>Undo Last Action</b> / <b>Redo Last Action</b>, alongside the <b>Ctrl+Z</b> / <b>Ctrl+Y</b> shortcuts from Section 7.<br><br>
<i>Pro Tip:</i> You can double-click a tag in the tree to instantly view all files associated with it.
````

# 5. Finding Data: The AST Filter Builder
````html
The File Explorer uses an Abstract Syntax Tree (AST) to filter files. Instead of a simple text search, you snap together logical blocks to build highly precise queries.
````

````html
At the top of the File Explorer is the Filter Track. Click <b>[ + Drop Tag or Click ]</b> to begin building a query.<br>
<ul>
    <li><b>Under Domain Of:</b> Finds all files holding this tag AND any of its children.</li>
    <li><b>Direct Element Of:</b> Finds ONLY files explicitly assigned this exact tag, ignoring children.</li>
    <li><b>AND / OR / SUBTRACT:</b> Logical operators to combine tags. Want to find files that are "Science" AND "2026" but SUBTRACT "Drafts"? Build the blocks, then click <b>Search</b>.</li>
    <li><b>Name Contains:</b> Matches files whose name contains the text you type in. In the <b>nStructure engine preview</b> this is a standalone block: drop it on its own to search every file in the lake by name, or put it under an <b>AND</b> next to a tag block to search within that tag. In the <b>current release</b> it instead wraps a block of its own — you must fill that inner slot with a tag first, and the filter stays empty until you do, so a name-only search is not available there.</li>
</ul>
<i>Tip:</i> You can also drag a tag straight from the Hierarchy Tree and drop it onto the Filter Track to add it as a new block instantly, instead of building it from the <b>[ + Drop Tag or Click ]</b> menu.<br><br>
If you build a very useful, complex filter, click <b>Save</b> to permanently generate a new Tag that holds the results of that exact query. You can also <b>Export</b> the filter's current results directly from the Filter Track.
````

# 6. Advanced Organization Perspectives
````html
Because files can hold infinite tags, you can create "Classification Perspectives" to sort your archive from entirely different angles without disrupting your primary tree.
````

````html
Don't just tag by subject. Create parallel hierarchies at the root level:<br>
<ul>
    <li><b>By Extension:</b> Create a parent tag called 'FileTypes', with children like 'PDF', 'JPG', and 'TXT'.</li>
    <li><b>By Status:</b> Create a parent tag called 'Workflow', with children like 'To-Do', 'In Progress', and 'Archived'.</li>
</ul>
To see which perspectives a file belongs to, <b>click the file once</b> in the Explorer — its full tag list opens as a popup menu. (The root <b>"File"</b> tag is left out, since every file carries it.) This works the same way in both the current release and the nStructure engine preview.
````

# 7. System Maintenance & Telemetry
````html
Onto Directory tracks your actions safely and allows you to reverse mistakes. It also includes an Optimization protocol to keep your graph running at lightning speed.
 If you accidentally delete a tag, delete an edge, or rename a file, simply press <b>Ctrl+Z</b> to undo it, or <b>Ctrl+Y</b> to redo it.<br><br>
````

````html
Be careful about <b>Undo / Redo</b>: right now every action is stored as its own individual record, so a bulk operation (importing 100 files, or tagging 100 files at once) can use up a lot of history in one step.<br><br>
In the current release, undo history is capped at your last 100 actions, and redo separately at 33 — so a long chain of undos cannot be fully redone. In the nStructure engine preview, history is currently unbounded — it keeps growing until memory runs out, so save/close periodically during very large sessions rather than relying on the cap to protect you.<br><br>
<b>Database Optimization (Vacuum):</b> When you delete tags, the system leaves "tombstones" (empty spaces) in the database to keep your graph running quickly. Over months of use, these empty spaces can add up. In the current release, click the <b>Optimize</b> button on your Data Lake header to safely defragment the graph and reclaim memory. The nStructure engine preview does not support this yet — its underlying engine has no compaction capability, so the button is hidden there rather than shown broken.
````