import os
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.pdfgen import canvas

def get_code_files(directory, extensions):
    """Fetch all code files from the given directory, skipping compiled folders."""
    code_files = {}
    excluded_dirs = {"target", ".idea", ".mvn", "node_modules", "build"}

    print("\n📋 List of files included in the PDF:\n" + "-" * 40)

    for root, dirs, files in os.walk(directory):
        # Skip excluded folders
        dirs[:] = [d for d in dirs if d not in excluded_dirs]

        for file in files:
            if any(file.endswith(ext) for ext in extensions):
                file_path = os.path.join(root, file)
                try:
                    with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                        code_files[file_path] = f.readlines()
                        print(f"✅ {file_path}")
                except Exception as e:
                    print(f"❌ Error reading {file_path}: {e}")
    return code_files

def create_pdf(code_data, output_pdf="example2_backend.pdf"):
    c = canvas.Canvas(output_pdf, pagesize=A4)
    width, height = A4
    margin = 20 * mm
    line_height = 10
    y = height - margin
    max_width = width - 2 * margin

    c.setFont("Courier", 8)

    for file_path, lines in code_data.items():
        # Page break if needed
        if y < margin + 2 * line_height:
            c.showPage()
            c.setFont("Courier", 8)
            y = height - margin

        # File header
        c.setFont("Helvetica-Bold", 10)
        
        # Wrap long file paths that might exceed page width
        if c.stringWidth(f"File: {file_path}") > max_width:
            parts = []
            current = "File: "
            for part in file_path.split(os.sep):
                if c.stringWidth(current + os.sep + part) > max_width:
                    parts.append(current)
                    current = part
                else:
                    if current == "File: ":
                        current += part
                    else:
                        current += os.sep + part
            parts.append(current)
            
            for i, part in enumerate(parts):
                c.drawString(margin, y, part)
                y -= line_height
        else:
            c.drawString(margin, y, f"File: {file_path}")
            y -= line_height
        
        c.setFont("Courier", 8)

        for line in lines:
            line = line.strip("\n").encode("latin-1", "replace").decode("latin-1")

            # Skip drawing empty lines, but still move y position
            if not line:
                y -= line_height
                continue

            # Improved text wrapping algorithm
            text_width = c.stringWidth(line)
            
            if text_width <= max_width:
                # Line fits on page
                c.drawString(margin, y, line)
                y -= line_height
            else:
                # Line needs wrapping
                words = line.split()
                if not words:  # Empty line
                    y -= line_height
                    continue
                    
                # Handle cases with very long words
                if len(words) == 1 or c.stringWidth(words[0]) > max_width:
                    current_line = ""
                    for char in line:
                        test_line = current_line + char
                        if c.stringWidth(test_line) <= max_width:
                            current_line = test_line
                        else:
                            # Draw current line and start a new one
                            if y < margin:
                                c.showPage()
                                c.setFont("Courier", 8)
                                y = height - margin
                            c.drawString(margin, y, current_line)
                            y -= line_height
                            current_line = char
                    
                    # Draw the last line if there's anything left
                    if current_line:
                        if y < margin:
                            c.showPage()
                            c.setFont("Courier", 8)
                            y = height - margin
                        c.drawString(margin, y, current_line)
                        y -= line_height
                else:
                    # Normal word wrapping
                    current_line = words[0]
                    for word in words[1:]:
                        test_line = current_line + " " + word
                        if c.stringWidth(test_line) <= max_width:
                            current_line = test_line
                        else:
                            # Draw current line and start a new one
                            if y < margin:
                                c.showPage()
                                c.setFont("Courier", 8)
                                y = height - margin
                            c.drawString(margin, y, current_line)
                            y -= line_height
                            current_line = word
                    
                    # Draw the last line if there's anything left
                    if current_line:
                        if y < margin:
                            c.showPage()
                            c.setFont("Courier", 8)
                            y = height - margin
                        c.drawString(margin, y, current_line)
                        y -= line_height

            # Check for page break after each line
            if y < margin:
                c.showPage()
                c.setFont("Courier", 8)
                y = height - margin

        # Extra space between files
        y -= line_height

    c.save()
    print(f"\n📄 PDF successfully created: {output_pdf}")

if __name__ == "__main__":
    # Use current directory where extract.py is located
    project_root = os.path.dirname(os.path.abspath(__file__))
    extensions = {".java", ".xml"}

    code_files = get_code_files(project_root, extensions)
    create_pdf(code_files)