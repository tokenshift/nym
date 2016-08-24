#!/usr/bin/env ruby

require 'nokogiri'
require 'open-uri'
require 'set'
require 'uri'

# For debugging, specify absolute URLs on the command line. Only these will
# be crawled; no links will be followed.
CRAWL_URLS = ARGV[0..-1]

# Start crawling here.
START_URLS = %w[
http://www.behindthename.com/names/list.php
http://surnames.behindthename.com/names/list
]

# Follow links matching any of these patterns.
FOLLOW_PATTERNS = [
  %r{^http://(www|surnames).behindthename.com/names/},
  %r{^http://(www|surnames).behindthename.com/name/}
]

# Dont' follow links matching any of these patterns (overrides FOLLOW_PATTERNS).
STOP_PATTERNS = [
  %r{/name/.*?/comments},
  %r{/name/.*?/images},
  %r{/name/.*?/namedays},
  %r{/name/.*?/namesakes},
  %r{/name/.*?/notes},
  %r{/name/.*?/rating},
  %r{/name/.*?/references},
  %r{/name/.*?/related},
  %r{/name/.*?/submitted},
  %r{/name/.*?/top},
  %r{/name/.*?/top/.*},
  %r{/name/.*?/tree},
  %r{/name/.*?/websites},
  %r{/contribute.php}
]

crawled = Set.new
backlog = if CRAWL_URLS.empty? then Set.new(START_URLS) else Set.new(CRAWL_URLS) end

until backlog.empty?
  url = backlog.first
  STDERR.puts "CRAWLING (#{crawled.length}/#{backlog.length+crawled.length}) #{url}"

  backlog.delete(url)
  crawled.add(url)

  doc = Nokogiri::HTML(open(url))

  # Look for name info on the page.
  if %r{http://(www|surnames).behindthename.com/name/[^/]*$}.match(url)
    name = %r{of the (?:sur)?name (.*)}i.match(doc.title)[1].strip

    if /surnames/.match(url)
      puts "#{name}\nSurname"
    end

    doc.css(".nameinfo > .namesub").each do |info|
      key = info.css(".namesub").text.strip
      val = info.css(".info").text.strip

      case key
      when "GENDER:"
        puts "#{name}\nMale" if /Masculine/.match(val)
        puts "#{name}\nFemale" if /Feminine/.match(val)
      when "USAGE:"
        # Turns a info string like "Mythology, Greek Mythology (Latinized)"
        # into a set of tags: [Mythology, Greek Mythology, Latinized].
        for tag in val.scan(/[^\(\),]+/).map(&:strip)
          puts "#{name}\n#{tag}"
        end
      end
    end
  end

  next unless CRAWL_URLS.empty?

  # Find all followable links.
  doc.css("a[href]").each do |a|
    absolute = URI.join(url, a["href"])
    absolute.fragment = nil
    absolute = absolute.to_s

    follow = false
    for follow_pattern in FOLLOW_PATTERNS
      follow = follow_pattern.match(absolute)
      break if follow
    end

    if follow
      for stop_pattern in STOP_PATTERNS
        follow = follow && !stop_pattern.match(absolute)
        break unless follow
      end
    end

    next if !follow
    next if crawled.include? absolute

    if backlog.add?(absolute)
      STDERR.puts "BACKLOG << #{absolute}"
    end
  end
end
