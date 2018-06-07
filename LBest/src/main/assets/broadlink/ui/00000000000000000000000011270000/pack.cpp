#include <stdio.h>
#include <string>
#include <fstream>
#include <sstream>
#include <iostream>
#include <vector>


#define COUNTOF(x) (sizeof(x)/sizeof((x)[0]))
using std::cout;
using std::endl;
using std::string;

std::string getfilecontent(const char *fname) {
	std::fstream fs;
	std::string s;
	try {
		std::stringstream ss;
		fs.open(fname, std::fstream::in | std::fstream::binary);
		ss << fs.rdbuf();
		fs.close();
		s = ss.str();
	} catch (...) {
		cout << "read " << fname << " failed" << endl;
		throw;
	}
	cout << fname << " read " << s.size() << " bytes" << endl;
	return s;
}

struct Platform {
	const char *csss[20];
	size_t csslen;
	const char *jss[20];
	size_t jsslen;
	const char *target;
};
int main(int argc, const char **argv) {
	if (argc != 2) {
		cout << "usage: pack --platform" << endl << "       where platform is [jd|bl|wx|fake]" << endl;
		return 0;
	}

	const char **csss = nullptr;
	size_t css_len = 0;
	const char **jss = nullptr;
	size_t js_len = 0;
	const char *target = nullptr;

	string src = "app.html";
	const char *faketarget = "app.fake.html";

	string platform(argv[1]);
	size_t _ = platform.find_last_of('-');
	if (_ != string::npos) {
		platform = platform.substr(_ + 1);
	}
	if (platform == "../platform/jd") {
		const char *_csss[] = {"../uikit.css", "app.css"};
		css_len = COUNTOF(_csss);
		csss = _csss;
		const char *_jss[] = {"../profile.js", "strings.js", "../q.min.js", "../platform/jd/jdsmart.js", "../platform/jd/adapter.js", "../jssdk.js", "../uikit.js", "app.js"};
		js_len = COUNTOF(_jss);
		jss = _jss;
		target = "app.jd.html";
	} else if (platform == "bl") {
		const char *_csss[] = {"../uikit.css", "app.css"};
		css_len = COUNTOF(_csss);
		csss = _csss;
		const char *_jss[] = {"../profile.js", "strings.js", "../q.min.js", "../platform/bl/adapter.js", "../jssdk.js", "../uikit.js", "app.js"};
		js_len = COUNTOF(_jss);
		jss = _jss;
		target = "app.bl.html";
	} else if (platform == "fake") {
		const char *_csss[] = {"../uikit.css", "app.css"};
		css_len = COUNTOF(_csss);
		csss = _csss;
		const char *_jss[] = {"../profile.js", "strings.js", "../q.min.js", "../platform/fake/adapter.js", "../jssdk.js", "../uikit.js", "app.js"};
		js_len = COUNTOF(_jss);
		jss = _jss;
		target = "app.fake.html";
	} else if (platform == "wx") {
		const char *_csss[] = {"../uikit.css", "app.css"};
		css_len = COUNTOF(_csss);
		csss = _csss;
		const char *_jss[] = {"../profile.js", "strings.js", "../q.min.js", "../platform/wx/adapter.js", "../jssdk.js", "../uikit.js", "app.js"};
		js_len = COUNTOF(_jss);
		jss = _jss;
		target = "app.wx.html";
	} else if (platform == "ali") {
		const char *_csss[] = {"../uikit.css", "app.css"};
		css_len = COUNTOF(_csss);
		csss = _csss;
		const char *_jss[] = {"../profile.js", "strings.js", "../q.min.js", "../platform/ali/alisdk.js", "../platform/ali/adapter.js", "../jssdk.js", "../uikit.js", "app.js"};
		js_len = COUNTOF(_jss);
		jss = _jss;
		target = "app.ali.html";
	} else {
		cout << "Platform: " << platform << " is not supported" << endl;
		return 0;
	}

	if (src == target) {
		cout << "target is the same with " << src << endl;
		return 0;
	}

	std::string css = "";
	for (int i = 0; i < css_len; ++ i) {
		css += getfilecontent(csss[i]);
	}

	std::string js = "";
	for (int i = 0; i < js_len; ++ i) {
		js += getfilecontent(jss[i]);
	}

	std::string html = getfilecontent(src.c_str());
	std::string tags[] = {"<!-- pack:css -->", "<!-- endpack -->", "<!-- pack:js -->", "<!-- endpack -->"};
	for (int i = 0; i < COUNTOF(tags) / 2; ++ i) {
		size_t idx = html.find(tags[i * 2]);
		if (idx == string::npos) {
			cout << "--- failed ---" << endl << src << " should have " << tags[i * 2] << endl;
			return 0;
		}
		idx = html.find(tags[i * 2 + 1]);
		if (idx == string::npos) {
			cout << "--- failed ---" << endl << src << " should have " << tags[i * 2 + 1] << endl;
			return 0;
		}
	}

	size_t idx = html.find(tags[0]);
	std::string _html = html.substr(0, idx + tags[0].size());
	_html += "\n<style>\n";
	_html += css;
	_html += "</style>\n";
	_html += html.substr(html.find(tags[1], idx + tags[0].size()), html.size());

	html = _html;

	idx = html.find(tags[2]);
	_html = html.substr(0, idx + tags[2].size());
	_html += "\n<script type=\"text/javascript\">\n";
	_html += js;
	_html += "</script>\n";
	_html += html.substr(html.find(tags[3], idx + tags[2].size()), html.size());
	cout << "html processed: " << _html.size() << " bytes" << endl;

	std::fstream fs;
	fs.open(target, std::fstream::out | std::fstream::binary);
	fs << _html;
	fs.close();

	std::cout << "Target html is generated successfully." << std::endl;
	std::cout << "generated:\t[" << target << "]\nfile size:\t[" << _html.size() << " B]" << std::endl;

	return 0;
}
